package com.arang.intake.dao;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.arang.intake.model.Event;

/**
 * Event DAO (the universal unit) — explicit, tunable SQL via {@link NamedParameterJdbcTemplate}.
 * Covers P1: insert (returns generated id), load, field/publish/geo updates, delete, and the
 * stale-draft cleanup query (REQ-18).
 */
@Repository
public class EventDao {

    private static final RowMapper<Event> MAPPER = (rs, n) -> {
        Event e = new Event();
        e.setId(rs.getLong("id"));
        e.setEventTypeId((Long) rs.getObject("event_type_id"));
        e.setBusinessId((Long) rs.getObject("business_id"));
        e.setTitle(rs.getString("title"));
        Timestamp sa = rs.getTimestamp("starts_at");
        e.setStartsAt(sa == null ? null : sa.toLocalDateTime());
        Timestamp ea = rs.getTimestamp("ends_at");
        e.setEndsAt(ea == null ? null : ea.toLocalDateTime());
        e.setLocation(rs.getString("location"));
        e.setPostalCode(rs.getString("postal_code"));
        e.setCountryCode(rs.getString("country_code"));
        e.setLatitude(rs.getBigDecimal("latitude"));
        e.setLongitude(rs.getBigDecimal("longitude"));
        e.setSourceType(rs.getString("source_type"));
        e.setRawMessage(rs.getString("raw_message"));
        e.setMediaRef(rs.getString("media_ref"));
        e.setExtractedConfidence(rs.getBigDecimal("extracted_confidence"));
        e.setStatus(rs.getString("status"));
        e.setOriginType(rs.getString("origin_type"));
        e.setSubmittedBy((Long) rs.getObject("submitted_by"));
        e.setIngestionSourceId((Long) rs.getObject("ingestion_source_id"));
        e.setApiClientId((Long) rs.getObject("api_client_id"));
        e.setEditToken(rs.getString("edit_token"));
        Timestamp exp = rs.getTimestamp("edit_token_expires_at");
        e.setEditTokenExpiresAt(exp == null ? null : exp.toLocalDateTime());
        return e;
    };

    private static final String COLS =
        "id, event_type_id, business_id, title, starts_at, ends_at, location, postal_code, " +
        "country_code, latitude, longitude, source_type, raw_message, media_ref, " +
        "extracted_confidence, status, origin_type, submitted_by, ingestion_source_id, " +
        "api_client_id, edit_token, edit_token_expires_at";

    private final NamedParameterJdbcTemplate jdbc;

    public EventDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Insert a new event; populates the generated id on the POJO and returns it. */
    public Event insert(Event e) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update("""
            INSERT INTO event
              (event_type_id, business_id, title, starts_at, ends_at, location, postal_code,
               country_code, latitude, longitude, source_type, raw_message, media_ref,
               extracted_confidence, status, origin_type, submitted_by, ingestion_source_id,
               api_client_id, edit_token, edit_token_expires_at, created_at, updated_at)
            VALUES
              (:eventTypeId, :businessId, :title, :startsAt, :endsAt, :location, :postalCode,
               :countryCode, :latitude, :longitude, :sourceType, :rawMessage, :mediaRef,
               :extractedConfidence, :status, :originType, :submittedBy, :ingestionSourceId,
               :apiClientId, :editToken, :editTokenExpiresAt, :createdAt, :updatedAt)
            """, params(e), keys, new String[] { "id" });
        e.setId(keys.getKey().longValue());
        return e;
    }

    /** Update the mutable columns of an existing event (edits, publish, geo, status). */
    public void update(Event e) {
        e.touchUpdatedAt();
        jdbc.update("""
            UPDATE event SET
              event_type_id = :eventTypeId, business_id = :businessId, title = :title,
              starts_at = :startsAt, ends_at = :endsAt, location = :location,
              postal_code = :postalCode, country_code = :countryCode, latitude = :latitude,
              longitude = :longitude, source_type = :sourceType, raw_message = :rawMessage,
              media_ref = :mediaRef, extracted_confidence = :extractedConfidence, status = :status,
              origin_type = :originType, submitted_by = :submittedBy,
              ingestion_source_id = :ingestionSourceId, api_client_id = :apiClientId,
              edit_token = :editToken, edit_token_expires_at = :editTokenExpiresAt,
              updated_at = :updatedAt
            WHERE id = :id
            """, params(e).addValue("id", e.getId()));
    }

    public Optional<Event> findById(Long id) {
        List<Event> found = jdbc.query("SELECT " + COLS + " FROM event WHERE id = :id",
            Map.of("id", id), MAPPER);
        return found.stream().findFirst();
    }

    public List<Event> findAll() {
        return jdbc.getJdbcTemplate().query("SELECT " + COLS + " FROM event", MAPPER);
    }

    public void deleteById(Long id) {
        jdbc.update("DELETE FROM event WHERE id = :id", Map.of("id", id));
    }

    /** Draft events created before the cutoff — candidates for cleanup (REQ-18). */
    public List<Event> findStaleDrafts(Instant cutoff) {
        return jdbc.query(
            "SELECT " + COLS + " FROM event WHERE status = 'draft' AND created_at < :cutoff",
            Map.of("cutoff", Timestamp.from(cutoff)), MAPPER);
    }

    public long count() {
        Long n = jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM event", Long.class);
        return n == null ? 0 : n;
    }

    public void deleteAll() {
        jdbc.getJdbcTemplate().update("DELETE FROM event");
    }

    private static MapSqlParameterSource params(Event e) {
        return new MapSqlParameterSource()
            .addValue("eventTypeId", e.getEventTypeId())
            .addValue("businessId", e.getBusinessId())
            .addValue("title", e.getTitle())
            .addValue("startsAt", e.getStartsAt())
            .addValue("endsAt", e.getEndsAt())
            .addValue("location", e.getLocation())
            .addValue("postalCode", e.getPostalCode())
            .addValue("countryCode", e.getCountryCode())
            .addValue("latitude", e.getLatitude())
            .addValue("longitude", e.getLongitude())
            .addValue("sourceType", e.getSourceType())
            .addValue("rawMessage", e.getRawMessage())
            .addValue("mediaRef", e.getMediaRef())
            .addValue("extractedConfidence", e.getExtractedConfidence())
            .addValue("status", e.getStatus())
            .addValue("originType", e.getOriginType())
            .addValue("submittedBy", e.getSubmittedBy())
            .addValue("ingestionSourceId", e.getIngestionSourceId())
            .addValue("apiClientId", e.getApiClientId())
            .addValue("editToken", e.getEditToken())
            .addValue("editTokenExpiresAt", e.getEditTokenExpiresAt())
            .addValue("createdAt", Timestamp.from(e.getCreatedAt() == null ? Instant.now() : e.getCreatedAt()))
            .addValue("updatedAt", Timestamp.from(Instant.now()));
    }
}
