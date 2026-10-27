package com.arang.intake.dao;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Typed-core per-field confidence (DD-1 table {@code event_field_confidence}). Explicit SQL;
 * upsert-by-(event,field) so re-enrichment overwrites a prior value.
 */
@Repository
public class EventFieldConfidenceDao {

    private final NamedParameterJdbcTemplate jdbc;

    public EventFieldConfidenceDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Insert or update the confidence for one typed-core field of an event. */
    public void upsert(Long eventId, String field, Double confidence) {
        int updated = jdbc.update("""
            UPDATE event_field_confidence SET confidence = :c
            WHERE event_id = :e AND field = :f
            """, Map.of("c", confidence, "e", eventId, "f", field));
        if (updated == 0) {
            jdbc.update("""
                INSERT INTO event_field_confidence (event_id, field, confidence)
                VALUES (:e, :f, :c)
                """, new MapSqlParameterSource()
                    .addValue("e", eventId).addValue("f", field).addValue("c", confidence));
        }
    }

    public Double find(Long eventId, String field) {
        var rows = jdbc.query(
            "SELECT confidence FROM event_field_confidence WHERE event_id = :e AND field = :f",
            Map.of("e", eventId, "f", field),
            (rs, n) -> rs.getBigDecimal("confidence") == null ? null : rs.getBigDecimal("confidence").doubleValue());
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void deleteAll() {
        jdbc.getJdbcTemplate().update("DELETE FROM event_field_confidence");
    }
}
