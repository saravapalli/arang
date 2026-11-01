package com.arang.intake.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.arang.intake.dao.EventDao;
import com.arang.intake.dao.EventFieldConfidenceDao;
import com.arang.intake.extract.EventExtractor;
import com.arang.intake.extract.ExtractedFields;
import com.arang.intake.model.Event;

/**
 * Best-effort enrichment of a draft using the {@link EventExtractor} (LLM when enabled). Runs
 * {@link Async} so a slow/failed model never blocks intake — the draft already exists; this only
 * fills typed-core fields the producer would otherwise type on the review page. Only fills fields
 * that are still empty (never overwrites a value the producer may have already set).
 */
@Service
public class EventEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(EventEnrichmentService.class);

    private final EventExtractor extractor;
    private final EventDao events;
    private final EventFieldConfidenceDao fieldConfidence;
    private final com.arang.intake.dao.EventMetadataDao metadata;
    private final com.arang.intake.dao.CategoryTagDao categoryTags;

    public EventEnrichmentService(EventExtractor extractor, EventDao events,
                                  EventFieldConfidenceDao fieldConfidence,
                                  com.arang.intake.dao.EventMetadataDao metadata,
                                  com.arang.intake.dao.CategoryTagDao categoryTags) {
        this.extractor = extractor;
        this.events = events;
        this.fieldConfidence = fieldConfidence;
        this.metadata = metadata;
        this.categoryTags = categoryTags;
    }

    private void metaFlag(Long eventId, String key, String value) {
        metadata.insert(new com.arang.intake.model.EventMetadata(eventId, key, value, "text", null, "system"));
    }

    /** Fire-and-forget enrichment after a draft is created. */
    @Async
    public void enrichAsync(Long eventId, String message) {
        try {
            enrich(eventId, message);
        } catch (Exception e) {
            log.warn("Enrichment failed for event {}: {}", eventId, e.toString());
        }
    }

    /** Fire-and-forget multimodal enrichment of an image draft (flyer). */
    @Async
    public void enrichFromImageAsync(Long eventId, byte[] imageBytes, String mimeType, String caption) {
        try {
            enrichFromImage(eventId, imageBytes, mimeType, caption);
        } catch (Exception e) {
            log.warn("Image enrichment failed for event {}: {}", eventId, e.toString());
        }
    }

    /** Synchronous core (also directly callable in tests). Returns true if anything was filled. */
    @Transactional
    public boolean enrich(Long eventId, String message) {
        return applyExtraction(eventId, extractor.extract(message));
    }

    /** Multimodal core: read the flyer image and apply fields. */
    @Transactional
    public boolean enrichFromImage(Long eventId, byte[] imageBytes, String mimeType, String caption) {
        return applyExtraction(eventId, extractor.extractFromImage(imageBytes, mimeType, caption));
    }

    /** Apply an {@link ExtractedFields} result to a draft (shared by text + image paths). */
    private boolean applyExtraction(Long eventId, ExtractedFields f) {
        if (!f.relevant()) {
            // Garbage / non-event: capture is lossless (draft still exists), but flag it so the
            // review surface / later triage can see it wasn't understood as an event.
            if (f.irrelevantReason() != null) {
                events.findById(eventId).ifPresent(e ->
                    metaFlag(eventId, "llm_relevance", "not-an-event: " + f.irrelevantReason()));
            }
            return false;
        }
        if (!f.hasAnyField()) {
            return false;
        }
        Optional<Event> found = events.findById(eventId);
        if (found.isEmpty()) {
            return false;
        }
        Event e = found.get();
        boolean changed = false;

        if (isBlank(e.getTitle()) && f.title() != null) {
            e.setTitle(f.title());
            recordConfidence(eventId, "title", f.titleConfidence());
            changed = true;
        }
        if (e.getStartsAt() == null && f.startsAt() != null) {
            e.setStartsAt(f.startsAt());
            recordConfidence(eventId, "starts_at", f.startsAtConfidence());
            changed = true;
        }
        if (isBlank(e.getLocation()) && f.location() != null) {
            e.setLocation(f.location());
            recordConfidence(eventId, "location", f.locationConfidence());
            changed = true;
        }

        // Harvest extra labeled details (meaningful keys) into event_metadata — dedupe by key so
        // re-enrichment doesn't duplicate rows.
        if (!f.attributes().isEmpty()) {
            var existingKeys = metadata.findByEventId(eventId).stream()
                .map(com.arang.intake.model.EventMetadata::getKey)
                .collect(java.util.stream.Collectors.toSet());
            for (var attr : f.attributes().entrySet()) {
                if (!existingKeys.contains(attr.getKey())) {
                    metadata.insert(new com.arang.intake.model.EventMetadata(
                        eventId, attr.getKey(), attr.getValue(), "text", null, "llm"));
                    changed = true;
                }
            }
        }

        // Backfill raw_message with the full read text (esp. flyer OCR) if we have none yet.
        if (isBlank(e.getRawMessage()) && f.rawText() != null && !f.rawText().isBlank()) {
            e.setRawMessage(f.rawText());
            changed = true;
        }

        // Categorization (REQ-15/16/17): link curated categories (vocab-validated) + open tags.
        if (!f.categories().isEmpty()) {
            categoryTags.linkCategories(eventId, f.categories());
            changed = true;
        }
        if (!f.tags().isEmpty()) {
            categoryTags.linkTags(eventId, f.tags());
            changed = true;
        }

        if (changed) {
            rollUpConfidence(e);
            events.update(e);
            log.info("Enriched event {} via LLM extractor", eventId);
        }
        return changed;
    }

    private void recordConfidence(Long eventId, String field, Double c) {
        if (c != null) {
            fieldConfidence.upsert(eventId, field, c);
        }
    }

    /** extracted_confidence = min of the field confidences we have (keeps REQ-06 semantics). */
    private void rollUpConfidence(Event e) {
        Double min = null;
        for (String field : new String[] { "title", "starts_at", "location" }) {
            Double c = fieldConfidence.find(e.getId(), field);
            if (c != null && (min == null || c < min)) {
                min = c;
            }
        }
        if (min != null) {
            BigDecimal existing = e.getExtractedConfidence();
            BigDecimal candidate = BigDecimal.valueOf(min);
            // keep the lower of any existing (e.g. URL) confidence and the new field min
            e.setExtractedConfidence(existing == null ? candidate : existing.min(candidate));
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
