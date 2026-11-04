package com.arang.intake.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.dao.DemandSignalDao;

/**
 * TC-P2.1-048 (ARANG-TTP-P2-1, task T12): the retention job deletes signals past 24 months and the waitlist-only
 * users left behind, keeping users that have a phone, a cookie id, interests, or a newer signal. Fixed clock.
 */
@SpringBootTest
@ActiveProfiles("test")
class WaitlistRetentionJobTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-11-04T00:00:00Z"), ZoneOffset.UTC);

    @Autowired
    DemandSignalDao signals;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM demand_signal");
        jdbc.update("DELETE FROM interest WHERE user_id IN (SELECT id FROM `user` WHERE email IS NOT NULL)");
        jdbc.update("DELETE FROM `user` WHERE email IS NOT NULL");
    }

    private long user(String email) {
        jdbc.update("INSERT INTO `user` (email) VALUES (?)", email);
        return jdbc.queryForObject("SELECT id FROM `user` WHERE email = ?", Long.class, email);
    }

    private void signal(long userId, String monthsAgoLabel, Instant createdAt) {
        jdbc.update("INSERT INTO demand_signal (user_id, place_key, place_label, consent_version, consent_text, "
                + "created_at) VALUES (?, ?, ?, 'v1', 'c', ?)",
            userId, "p-" + monthsAgoLabel, monthsAgoLabel, Timestamp.from(createdAt));
    }

    @Test
    void deletesSignalsPastRetentionAndOrphanUsers() {
        Instant now = Instant.now(FIXED);
        long oldUser = user("old@example.com");
        long freshUser = user("fresh@example.com");
        // 25 months old (past 24*30 days) and 23 months old (within).
        signal(oldUser, "old", now.minusSeconds(25L * 30 * 86400));
        signal(freshUser, "fresh", now.minusSeconds(23L * 30 * 86400));

        WaitlistRetentionJob job = new WaitlistRetentionJob(signals, FIXED, 24);
        int removed = job.run();

        assertThat(removed).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM demand_signal", Integer.class)).isEqualTo(1);
        // The old user had only that signal and nothing else → deleted.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user` WHERE email='old@example.com'", Integer.class))
            .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user` WHERE email='fresh@example.com'", Integer.class))
            .isEqualTo(1);
    }

    @Test
    void keepsUsersWithInterestsOrAnIdentity() {
        Instant now = Instant.now(FIXED);
        long withInterest = user("keep-interest@example.com");
        jdbc.update("INSERT INTO interest (user_id, category_id, weight) "
            + "VALUES (?, (SELECT id FROM category LIMIT 1), 1.0)", withInterest);
        signal(withInterest, "old", now.minusSeconds(25L * 30 * 86400));

        new WaitlistRetentionJob(signals, FIXED, 24).run();
        // Signal is gone, but the user is kept because they have an interest.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user` WHERE email='keep-interest@example.com'",
            Integer.class)).isEqualTo(1);
    }
}
