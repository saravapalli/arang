package com.arang.intake.model;

import java.time.Instant;

/**
 * Idempotency ledger row (SDD §1 {@code processed_message}, REQ-07). Plain POJO; persistence is
 * handled by {@code ProcessedMessageDao} with explicit SQL (no ORM).
 */
public class ProcessedMessage {

    private String providerMessageId;
    private Long eventId;
    private Instant processedAt = Instant.now();

    public ProcessedMessage() {
    }

    public ProcessedMessage(String providerMessageId) {
        this.providerMessageId = providerMessageId;
        this.processedAt = Instant.now();
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }
}
