package com.arang.intake.dao;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.arang.intake.model.ProcessedMessage;

/**
 * Idempotency ledger DAO (REQ-07) — explicit SQL via {@link NamedParameterJdbcTemplate}.
 * Existence of a row keyed by the provider message id means the message was already processed.
 */
@Repository
public class ProcessedMessageDao {

    private final NamedParameterJdbcTemplate jdbc;

    public ProcessedMessageDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsById(String providerMessageId) {
        Integer n = jdbc.queryForObject(
            "SELECT COUNT(*) FROM processed_message WHERE provider_message_id = :id",
            Map.of("id", providerMessageId), Integer.class);
        return n != null && n > 0;
    }

    public void insert(ProcessedMessage m) {
        jdbc.update("""
            INSERT INTO processed_message (provider_message_id, event_id, processed_at)
            VALUES (:id, :eventId, :processedAt)
            """,
            new MapSqlParameterSource()
                .addValue("id", m.getProviderMessageId())
                .addValue("eventId", m.getEventId())
                .addValue("processedAt", Timestamp.from(
                    m.getProcessedAt() == null ? Instant.now() : m.getProcessedAt())));
    }

    /** Link the ledger row to the event created from it (T3). */
    public void linkEvent(String providerMessageId, Long eventId) {
        jdbc.update("UPDATE processed_message SET event_id = :eventId WHERE provider_message_id = :id",
            Map.of("eventId", eventId, "id", providerMessageId));
    }

    public Long findEventId(String providerMessageId) {
        return jdbc.queryForObject(
            "SELECT event_id FROM processed_message WHERE provider_message_id = :id",
            Map.of("id", providerMessageId), Long.class);
    }

    public long count() {
        Long n = jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM processed_message", Long.class);
        return n == null ? 0 : n;
    }

    public void deleteAll() {
        jdbc.getJdbcTemplate().update("DELETE FROM processed_message");
    }
}
