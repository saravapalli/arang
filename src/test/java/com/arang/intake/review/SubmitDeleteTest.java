package com.arang.intake.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.model.Event;
import com.arang.intake.model.EventMetadata;
import com.arang.intake.dao.EventMetadataDao;
import com.arang.intake.dao.EventDao;
import com.arang.intake.service.ReviewService;
import com.arang.intake.service.ReviewService.SubmitResult;

import java.math.BigDecimal;

/** T6 — submit/publish/geo/delete (REQ-11, REQ-12, REQ-13). */
@SpringBootTest
@ActiveProfiles("test")
class SubmitDeleteTest {

    @Autowired private ReviewService reviewService;
    @Autowired private EventDao events;
    @Autowired private EventMetadataDao metadata;

    @BeforeEach
    void clean() {
        metadata.deleteAll();
        events.deleteAll();
    }

    private Event draft(String token, String postal) {
        Event e = new Event();
        e.setStatus("draft");
        e.setSourceType("text");
        e.setOriginType("user");
        e.setEditToken(token);
        e.setEditTokenExpiresAt(LocalDateTime.now().plusHours(1));
        e.setPostalCode(postal);
        return events.insert(e);
    }

    @Test
    void submitWithoutPostalIsRejectedAndNotPublished() {
        Event e = draft("tok", null);
        assertThat(reviewService.submit(e.getId(), "tok")).isEqualTo(SubmitResult.MISSING_POSTAL);
        assertThat(events.findById(e.getId()).orElseThrow().getStatus()).isEqualTo("draft");
    }

    @Test
    void submitWithKnownPostalPublishesAndDerivesGeo() {
        Event e = draft("tok", "01721"); // Ashland, MA — in the bundled dataset
        assertThat(reviewService.submit(e.getId(), "tok")).isEqualTo(SubmitResult.PUBLISHED);
        Event p = events.findById(e.getId()).orElseThrow();
        assertThat(p.getStatus()).isEqualTo("published");
        assertThat(p.getLatitude()).isNotNull();
        assertThat(p.getLongitude()).isNotNull();
    }

    @Test
    void submitWithUngeocodablePostalIsRejected() {
        Event e = draft("tok", "99999"); // not in the bundled centroid set → cannot be located
        // A published event must be displayable (pin + area match). An ungeocodable postal is
        // rejected rather than silently publishing an event that can never surface in discovery.
        assertThat(reviewService.submit(e.getId(), "tok")).isEqualTo(SubmitResult.UNGEOCODABLE_POSTAL);
        Event p = events.findById(e.getId()).orElseThrow();
        assertThat(p.getStatus()).isEqualTo("draft"); // stays a draft
        assertThat(p.getLatitude()).isNull();
    }

    @Test
    void nearbyAshlandAreaTownPublishesAndGeocodes() {
        // 01532 (Northborough) is an Ashland-area MA town — now covered, so it publishes + pins.
        Event e = draft("tok", "01532");
        assertThat(reviewService.submit(e.getId(), "tok")).isEqualTo(SubmitResult.PUBLISHED);
        Event p = events.findById(e.getId()).orElseThrow();
        assertThat(p.getStatus()).isEqualTo("published");
        assertThat(p.getLatitude()).isNotNull();
    }

    @Test
    void publishDoesNotLockEditing() {
        Event e = draft("tok", "01721");
        reviewService.submit(e.getId(), "tok");
        // edit after publish still works (AC-021a)
        boolean edited = reviewService.saveEdits(e.getId(), "tok", "New Title", null, null, "01721");
        assertThat(edited).isTrue();
        assertThat(events.findById(e.getId()).orElseThrow().getTitle()).isEqualTo("New Title");
    }

    @Test
    void submitWithBadTokenIsUnauthorized() {
        Event e = draft("tok", "01721");
        assertThat(reviewService.submit(e.getId(), "wrong")).isEqualTo(SubmitResult.UNAUTHORIZED);
    }

    @Test
    void deleteRemovesEventAndMetadataAndTokenStopsResolving() {
        Event e = draft("tok", "01721");
        metadata.insert(new EventMetadata(e.getId(), "Tickets", "https://x.com", "url", new BigDecimal("1.00"), "text"));

        boolean ok = reviewService.delete(e.getId(), "tok");
        assertThat(ok).isTrue();
        assertThat(events.findById(e.getId())).isEmpty();                 // event gone (REQ-12)
        assertThat(metadata.findByEventId(e.getId())).isEmpty();          // metadata gone
        assertThat(reviewService.authorize(e.getId(), "tok")).isEmpty();  // token no longer resolves
    }

    @Test
    void deleteWithBadTokenIsRejected() {
        Event e = draft("tok", "01721");
        assertThat(reviewService.delete(e.getId(), "wrong")).isFalse();
        assertThat(events.findById(e.getId())).isPresent(); // not deleted
    }
}
