package com.arang.intake.extract;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.dao.EventDao;
import com.arang.intake.dao.EventFieldConfidenceDao;
import com.arang.intake.dao.EventMetadataDao;
import com.arang.intake.model.Event;
import com.arang.intake.service.EventEnrichmentService;

/** Enrichment fills empty typed-core fields + confidences + attributes using the extractor. */
@SpringBootTest
@ActiveProfiles("test")
class EventEnrichmentServiceTest {

    @TestConfiguration
    static class StubExtractorConfig {
        @Bean @Primary
        EventExtractor stubExtractor() {
            return message -> new ExtractedFields(
                true, null,
                "Live Jazz", LocalDateTime.of(2026, 11, 6, 20, 0), "The Cellar", "concert",
                0.9, 0.8, 0.7,
                Map.of("phone", "773-610-8868", "instagram", "@rhythmbollywood"),
                "Live Jazz at The Cellar — call 773-610-8868");
        }
    }

    @Autowired private EventEnrichmentService enrichment;
    @Autowired private EventDao events;
    @Autowired private EventFieldConfidenceDao fieldConfidence;
    @Autowired private EventMetadataDao metadata;

    @BeforeEach
    void clean() {
        metadata.deleteAll();
        fieldConfidence.deleteAll();
        events.deleteAll();
    }

    private Event draft() {
        Event e = new Event();
        e.setStatus("draft");
        e.setSourceType("text");
        e.setOriginType("user");
        e.setRawMessage("Live jazz this Friday 8pm at The Cellar");
        return events.insert(e);
    }

    @Test
    void enrichFillsEmptyFieldsAndConfidences() {
        Event e = draft();
        boolean changed = enrichment.enrich(e.getId(), e.getRawMessage());

        assertThat(changed).isTrue();
        Event reloaded = events.findById(e.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Live Jazz");
        assertThat(reloaded.getLocation()).isEqualTo("The Cellar");
        assertThat(reloaded.getStartsAt()).isEqualTo(LocalDateTime.of(2026, 11, 6, 20, 0));
        assertThat(reloaded.getExtractedConfidence()).isEqualByComparingTo("0.70");
        assertThat(fieldConfidence.find(e.getId(), "title")).isEqualTo(0.9);
    }

    @Test
    void enrichStoresAttributesAsMetadataWithMeaningfulKeys() {
        Event e = draft();
        enrichment.enrich(e.getId(), e.getRawMessage());

        var md = metadata.findByEventId(e.getId());
        assertThat(md).anyMatch(m -> "phone".equals(m.getKey()) && "773-610-8868".equals(m.getValue())
            && "llm".equals(m.getSource()));
        assertThat(md).anyMatch(m -> "instagram".equals(m.getKey()) && "@rhythmbollywood".equals(m.getValue()));
    }

    @Test
    void enrichDoesNotDuplicateAttributesOnReRun() {
        Event e = draft();
        enrichment.enrich(e.getId(), e.getRawMessage());
        enrichment.enrich(e.getId(), e.getRawMessage()); // second pass
        long phoneRows = metadata.findByEventId(e.getId()).stream()
            .filter(m -> "phone".equals(m.getKey())).count();
        assertThat(phoneRows).isEqualTo(1); // deduped by key
    }

    @Test
    void enrichDoesNotOverwriteFieldsAlreadySet() {
        Event e = draft();
        e.setTitle("Producer's Own Title");
        events.update(e);

        enrichment.enrich(e.getId(), e.getRawMessage());

        Event reloaded = events.findById(e.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Producer's Own Title");
        assertThat(reloaded.getLocation()).isEqualTo("The Cellar");
    }
}
