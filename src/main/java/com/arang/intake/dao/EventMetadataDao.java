package com.arang.intake.dao;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.arang.intake.model.EventMetadata;

/** Per-event labeled metadata DAO (REQ-05) — explicit SQL. `key`/`value`/`signal`-style reserved
 * words are backtick-quoted. */
@Repository
public class EventMetadataDao {

    private static final RowMapper<EventMetadata> MAPPER = (rs, n) -> {
        EventMetadata m = new EventMetadata();
        m.setId(rs.getLong("id"));
        m.setEventId(rs.getLong("event_id"));
        m.setKey(rs.getString("key"));
        m.setValue(rs.getString("value"));
        m.setKind(rs.getString("kind"));
        m.setConfidence(rs.getBigDecimal("confidence"));
        m.setSource(rs.getString("source"));
        return m;
    };

    private final NamedParameterJdbcTemplate jdbc;

    public EventMetadataDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(EventMetadata m) {
        jdbc.update("""
            INSERT INTO event_metadata (event_id, `key`, `value`, kind, confidence, source, created_at)
            VALUES (:eventId, :key, :value, :kind, :confidence, :source, :createdAt)
            """,
            new MapSqlParameterSource()
                .addValue("eventId", m.getEventId())
                .addValue("key", m.getKey())
                .addValue("value", m.getValue())
                .addValue("kind", m.getKind())
                .addValue("confidence", m.getConfidence())
                .addValue("source", m.getSource())
                .addValue("createdAt", Timestamp.from(Instant.now())));
    }

    public List<EventMetadata> findByEventId(Long eventId) {
        return jdbc.query(
            "SELECT id, event_id, `key`, `value`, kind, confidence, source FROM event_metadata WHERE event_id = :eventId",
            Map.of("eventId", eventId), MAPPER);
    }

    public void deleteByEventId(Long eventId) {
        jdbc.update("DELETE FROM event_metadata WHERE event_id = :eventId", Map.of("eventId", eventId));
    }

    public long count() {
        Long n = jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM event_metadata", Long.class);
        return n == null ? 0 : n;
    }

    public void deleteAll() {
        jdbc.getJdbcTemplate().update("DELETE FROM event_metadata");
    }
}
