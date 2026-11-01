package com.arang.intake.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.dao.EventDao;
import com.arang.intake.extract.EventExtractor;
import com.arang.intake.extract.ExtractedFields;
import com.arang.intake.model.Event;
import com.arang.intake.service.EventEnrichmentService;

/**
 * S-E categorization (ARANG-TTP-P2): TC-18 (map to curated categories only — a bogus category is
 * dropped), TC-19 (mint an open tag). Uses a stub extractor returning categories + tags.
 */
@SpringBootTest
@ActiveProfiles("test")
class CategorizationEnrichmentTest {

    @TestConfiguration
    static class StubConfig {
        @Bean @Primary
        EventExtractor stubExtractor() {
            return message -> new ExtractedFields(
                true, null, "Garba/Dandiya Night", null, "Sri Lakshmi Temple", "festival",
                0.9, null, 0.8, Map.of(), "raw",
                List.of("family", "community", "not-a-real-category"), // curated + 1 bogus
                List.of("navaratri", "dandiya"));                      // open tags
        }
    }

    @Autowired EventEnrichmentService enrichment;
    @Autowired EventDao events;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM event_category");
        jdbc.update("DELETE FROM event_tag");
        jdbc.update("DELETE FROM tag");
        events.deleteAll();
    }

    private long draft() {
        Event e = new Event();
        e.setStatus("draft");
        e.setSourceType("text");
        e.setOriginType("user");
        return events.insert(e).getId();
    }

    @Test
    void mapsToCuratedCategoriesOnly() { // TC-18 / REQ-15 / REQ-17
        long id = draft();
        enrichment.enrich(id, "garba dandiya night at the temple");

        List<String> cats = jdbc.query(
            "SELECT c.name FROM event_category ec JOIN category c ON c.id=ec.category_id WHERE ec.event_id=?",
            (rs, n) -> rs.getString("name"), id);
        // the bogus category is NOT written; the two curated ones are.
        assertThat(cats).containsExactlyInAnyOrder("family", "community");
        assertThat(cats).doesNotContain("not-a-real-category");
    }

    @Test
    void mintsOpenTags() { // TC-19 / REQ-16
        long id = draft();
        enrichment.enrich(id, "garba dandiya night at the temple");

        List<String> tags = jdbc.query(
            "SELECT t.name FROM event_tag et JOIN tag t ON t.id=et.tag_id WHERE et.event_id=?",
            (rs, n) -> rs.getString("name"), id);
        assertThat(tags).containsExactlyInAnyOrder("navaratri", "dandiya");
    }
}
