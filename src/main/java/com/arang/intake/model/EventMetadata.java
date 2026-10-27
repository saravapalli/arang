package com.arang.intake.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Per-event labeled metadata (SDD §1 {@code event_metadata}). Plain POJO; persisted by
 * {@code EventMetadataDao} with explicit SQL. Raw source label as {@code key}, captured content as
 * {@code value}, light {@code kind} hint, per-field {@code confidence}, and {@code source}
 * (ocr|text|url).
 */
public class EventMetadata {

    private Long id;
    private Long eventId;
    private String key;
    private String value;
    private String kind;
    private BigDecimal confidence;
    private String source;
    private Instant createdAt = Instant.now();

    public EventMetadata() {
    }

    public EventMetadata(Long eventId, String key, String value, String kind,
                         BigDecimal confidence, String source) {
        this.eventId = eventId;
        this.key = key;
        this.value = value;
        this.kind = kind;
        this.confidence = confidence;
        this.source = source;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
