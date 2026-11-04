package com.arang.intake.dao;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Waitlist demand signals (ARANG-SDD-P2-1 §1.1, task T10). Idempotent: a repeat of the same
 * (user, kind, place) is a no-op thanks to the unique key. Records are stored unverified.
 */
@Repository
public class DemandSignalDao {

    private final NamedParameterJdbcTemplate jdbc;

    public DemandSignalDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Insert the signal, or do nothing if the same user already has this kind and place
     * ({@code ON DUPLICATE KEY UPDATE id = id} is a safe no-op). {@code verified_at} stays NULL.
     */
    public void upsert(long userId, String placeKey, String placeLabel, String consentVersion, String consentText) {
        jdbc.update("""
            INSERT INTO demand_signal (user_id, kind, place_key, place_label, consent_version, consent_text, created_at)
            VALUES (:userId, 'town_waitlist', :placeKey, :placeLabel, :consentVersion, :consentText, :now)
            ON DUPLICATE KEY UPDATE id = id
            """,
            new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("placeKey", placeKey)
                .addValue("placeLabel", placeLabel)
                .addValue("consentVersion", consentVersion)
                .addValue("consentText", consentText)
                .addValue("now", Timestamp.from(Instant.now())));
    }

    /** Delete signals created strictly before the cutoff. Returns the number removed (REQ-20). */
    public int deleteOlderThan(Instant cutoff) {
        return jdbc.update("DELETE FROM demand_signal WHERE created_at < :cutoff",
            new MapSqlParameterSource().addValue("cutoff", Timestamp.from(cutoff)));
    }

    /**
     * Delete users that exist only for the waitlist: no phone, no web cookie id, no interests, and no
     * remaining demand signal. Returns the number removed (REQ-20).
     */
    public int deleteOrphanWaitlistUsers() {
        return jdbc.getJdbcTemplate().update("""
            DELETE FROM `user`
            WHERE phone_number IS NULL AND external_ref IS NULL
              AND id NOT IN (SELECT user_id FROM demand_signal)
              AND id NOT IN (SELECT user_id FROM interest)
            """);
    }
}
