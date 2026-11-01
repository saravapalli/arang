package com.arang.intake.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.dao.EventDao;
import com.arang.intake.model.Event;
import com.arang.intake.service.ReviewService;

/** T12 / TC-20 — review-page category add/remove (REQ-18), token-gated. */
@SpringBootTest
@ActiveProfiles("test")
class ReviewCategoryTest {

    @Autowired ReviewService review;
    @Autowired EventDao events;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM event_category");
        events.deleteAll();
    }

    private Event draft(String token) {
        Event e = new Event();
        e.setStatus("draft");
        e.setSourceType("text");
        e.setOriginType("user");
        e.setEditToken(token);
        e.setEditTokenExpiresAt(LocalDateTime.now().plusHours(1));
        return events.insert(e);
    }

    @Test
    void addThenRemoveCategoryPersists() { // TC-20
        Event e = draft("tok-cat");

        assertThat(review.addCategory(e.getId(), "tok-cat", "music")).isTrue();
        assertThat(review.addCategory(e.getId(), "tok-cat", "family")).isTrue();
        assertThat(review.categoriesFor(e.getId())).containsExactlyInAnyOrder("music", "family");

        assertThat(review.removeCategory(e.getId(), "tok-cat", "music")).isTrue();
        assertThat(review.categoriesFor(e.getId())).containsExactly("family");
    }

    @Test
    void wrongTokenIsRejected() {
        Event e = draft("tok-cat");
        assertThat(review.addCategory(e.getId(), "wrong-token", "music")).isFalse();
        assertThat(review.categoriesFor(e.getId())).isEmpty();
    }
}
