package com.arang.intake.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.dao.EventDao;
import com.arang.intake.dao.UserDao;
import com.arang.intake.dto.DiscoveryResult;
import com.arang.intake.model.Event;
import com.arang.intake.model.User;
import com.arang.intake.service.ConsumerIdentityResolver;
import com.arang.intake.service.DiscoveryService;
import com.arang.intake.service.InterestService;

/**
 * S-C identity + interests (ARANG-TTP-P2): TC-11 (resolve phone/cookie/unrecognized),
 * TC-12 (external_ref find-or-create), TC-13 (save + vocab validation),
 * TC-14 (interest pre-filter), TC-15 (empty-interest fallback).
 */
@SpringBootTest
@ActiveProfiles("test")
class InterestIdentityTest {

    @Autowired ConsumerIdentityResolver identity;
    @Autowired InterestService interests;
    @Autowired DiscoveryService discovery;
    @Autowired UserDao users;
    @Autowired EventDao events;
    @Autowired JdbcTemplate jdbc;

    private static final LocalDateTime IN = LocalDateTime.of(2026, 10, 30, 20, 0, 0);
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 10, 26, 0, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 11, 1, 23, 59, 59);

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM interest");
        jdbc.update("DELETE FROM event_category");
        events.deleteAll();
        users.deleteAll();
    }

    @Test
    void resolvesPhoneCookieAndUnrecognized() { // TC-11
        User byPhone = identity.resolvePhone("+15551234567", "Sam");
        assertThat(byPhone.getId()).isNotNull();
        assertThat(identity.resolvePhone("+15551234567", "Sam").getId()).isEqualTo(byPhone.getId());

        User byCookie = identity.resolveWeb("cookie-abc");
        assertThat(byCookie.getExternalRef()).isEqualTo("cookie-abc");

        // Unrecognized: no identifier → empty, and no row created.
        assertThat(identity.recognize(null, null)).isEmpty();
        assertThat(identity.recognize(null, "never-seen")).isEmpty();
    }

    @Test
    void externalRefFindOrCreateIsStable() { // TC-12
        User first = identity.resolveWeb("cookie-xyz");
        User again = identity.resolveWeb("cookie-xyz");
        assertThat(again.getId()).isEqualTo(first.getId()); // no duplicate row
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void saveValidatesVocabularyThenReads() { // TC-13
        User u = identity.resolveWeb("cookie-1");
        interests.save(u.getId(), List.of("music", "food"));
        assertThat(interests.get(u.getId())).containsExactlyInAnyOrder("music", "food");

        assertThatThrownBy(() -> interests.save(u.getId(), List.of("not-a-category")))
            .isInstanceOf(InterestService.UnknownCategoryException.class);
    }

    private Event ev(String title, String cat) {
        Event e = new Event();
        e.setTitle(title);
        e.setStatus("published");
        e.setSourceType("text");
        e.setOriginType("user");
        e.setStartsAt(IN);
        e.setPostalCode("01721");
        e.setLatitude(new BigDecimal("42.2459"));
        e.setLongitude(new BigDecimal("-71.4665"));
        Event saved = events.insert(e);
        Long cid = jdbc.queryForObject("SELECT id FROM category WHERE name=?", Long.class, cat);
        jdbc.update("INSERT INTO event_category (event_id, category_id) VALUES (?,?)", saved.getId(), cid);
        return saved;
    }

    @Test
    void interestPreFilterNarrowsToSavedCategories() { // TC-14
        ev("Music one", "music");
        ev("Food one", "food");
        User u = identity.resolveWeb("cookie-2");
        interests.save(u.getId(), List.of("music"));

        // Simulate the controller's interest pre-filter: discovery with the saved interest.
        List<DiscoveryResult> r = discovery.find(FROM, TO, "01721", interests.get(u.getId()));
        assertThat(r).extracting(DiscoveryResult::title).containsExactly("Music one");
    }

    @Test
    void emptyInterestMeansUnfiltered() { // TC-15
        ev("Music one", "music");
        ev("Food one", "food");
        User u = identity.resolveWeb("cookie-3"); // no interests saved
        assertThat(interests.get(u.getId())).isEmpty();

        // Fallback: unfiltered in-window/in-area set (controller sets the flag).
        List<DiscoveryResult> r = discovery.find(FROM, TO, "01721", List.of());
        assertThat(r).extracting(DiscoveryResult::title).contains("Music one", "Food one");
    }
}
