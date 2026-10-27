package com.arang.intake.cleanup;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.model.Event;
import com.arang.intake.model.EventMetadata;
import com.arang.intake.dao.EventMetadataDao;
import com.arang.intake.dao.EventDao;
import com.arang.intake.service.CleanupJob;

/** T9 — scheduled draft cleanup (REQ-18). Tested via the parameterized cutoff (no timing waits). */
@SpringBootTest
@ActiveProfiles("test")
class CleanupJobTest {

    @Autowired private CleanupJob cleanupJob;
    @Autowired private EventDao events;
    @Autowired private EventMetadataDao metadata;

    @BeforeEach
    void clean() {
        metadata.deleteAll();
        events.deleteAll();
    }

    private Event save(String status) {
        Event e = new Event();
        e.setStatus(status);
        e.setSourceType("text");
        e.setOriginType("user");
        return events.insert(e);
    }

    @Test
    void deletesStaleDraftAndItsMetadataButLeavesPublished() {
        Event draft = save("draft");
        metadata.insert(new EventMetadata(draft.getId(), "k", "v", "text", new BigDecimal("1.00"), "text"));
        Event published = save("published");

        // cutoff in the future → everything created "now" counts as older than cutoff
        Instant future = Instant.now().plus(1, ChronoUnit.DAYS);
        int deleted = cleanupJob.deleteStaleDrafts(future);

        assertThat(deleted).isEqualTo(1);                               // only the draft
        assertThat(events.findById(draft.getId())).isEmpty();          // draft gone (REQ-18)
        assertThat(metadata.findByEventId(draft.getId())).isEmpty();   // its metadata gone
        assertThat(events.findById(published.getId())).isPresent();    // published untouched (REQ-18)
    }

    @Test
    void recentDraftIsNotDeleted() {
        Event draft = save("draft");
        // cutoff in the past → nothing created "now" is older than it
        Instant past = Instant.now().minus(365, ChronoUnit.DAYS);
        int deleted = cleanupJob.deleteStaleDrafts(past);

        assertThat(deleted).isZero();
        assertThat(events.findById(draft.getId())).isPresent();        // recent draft kept
    }
}
