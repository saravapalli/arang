package com.arang.intake.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.arang.intake.dao.EventDao;
import com.arang.intake.dao.EventMetadataDao;
import com.arang.intake.model.Event;

/**
 * Token-gated access to an event for review/edit/submit/delete (REQ-09..13).
 *
 * <p>A request is authorized iff the event exists, the presented token equals the event's
 * {@code edit_token}, and the token has not expired (REQ-17). All of edit/submit/delete go
 * through {@link #authorize} so the rule is enforced in exactly one place.
 */
@Service
public class ReviewService {

    /** Outcome of a submit attempt. */
    public enum SubmitResult { PUBLISHED, MISSING_POSTAL, UNAUTHORIZED }

    private final EventDao events;
    private final EventMetadataDao metadata;
    private final PostalGeoService postalGeo;

    public ReviewService(EventDao events, EventMetadataDao metadata,
                         PostalGeoService postalGeo) {
        this.events = events;
        this.metadata = metadata;
        this.postalGeo = postalGeo;
    }

    /** @return the event iff the token is valid, matches, and is unexpired; else empty. */
    @Transactional(readOnly = true)
    public Optional<Event> authorize(Long eventId, String token) {
        if (eventId == null || token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<Event> found = events.findById(eventId);
        if (found.isEmpty()) {
            return Optional.empty();                      // unknown event (REQ-10)
        }
        Event e = found.get();
        if (e.getEditToken() == null || !e.getEditToken().equals(token)) {
            return Optional.empty();                      // missing/invalid token (REQ-10)
        }
        LocalDateTime exp = e.getEditTokenExpiresAt();
        if (exp != null && exp.isBefore(LocalDateTime.now())) {
            return Optional.empty();                      // expired (REQ-10 / REQ-17)
        }
        return found;
    }

    /** Apply field edits after authorization; saving sets updated_at (REQ-10). */
    @Transactional
    public boolean saveEdits(Long eventId, String token, String title, String startsAt,
                             String location, String postalCode) {
        Optional<Event> authorized = authorize(eventId, token);
        if (authorized.isEmpty()) {
            return false;
        }
        Event e = authorized.get();
        e.setTitle(blankToNull(title));
        e.setStartsAt(parseDateTime(startsAt));
        e.setLocation(blankToNull(location));
        e.setPostalCode(blankToNull(postalCode));
        e.touchUpdatedAt();
        events.update(e);
        return true;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /**
     * Submit/publish (REQ-11, REQ-13): requires {@code postal_code}; on success sets
     * {@code status=published} and derives approximate lat/long from the postal centroid.
     * Publishing does NOT lock editing within the token window (AC-021a). Idempotent on re-submit.
     */
    @Transactional
    public SubmitResult submit(Long eventId, String token) {
        Optional<Event> authorized = authorize(eventId, token);
        if (authorized.isEmpty()) {
            return SubmitResult.UNAUTHORIZED;
        }
        Event e = authorized.get();
        if (e.getPostalCode() == null || e.getPostalCode().isBlank()) {
            return SubmitResult.MISSING_POSTAL;            // REQ-11: cannot publish without postal
        }
        if (e.getCountryCode() == null || e.getCountryCode().isBlank()) {
            e.setCountryCode("US");                        // REQ-13 default
        }
        postalGeo.deriveCentroid(e.getPostalCode(), e.getCountryCode()).ifPresent(latlon -> {
            e.setLatitude(latlon[0]);
            e.setLongitude(latlon[1]);
        });
        e.setStatus("published");
        e.touchUpdatedAt();
        events.update(e);
        return SubmitResult.PUBLISHED;
    }

    /**
     * Delete (REQ-12): remove the event and its metadata; afterwards the token no longer resolves
     * (the event is gone, so {@link #authorize} returns empty).
     */
    @Transactional
    public boolean delete(Long eventId, String token) {
        Optional<Event> authorized = authorize(eventId, token);
        if (authorized.isEmpty()) {
            return false;
        }
        metadata.deleteByEventId(eventId);
        events.deleteById(eventId);
        return true;
    }

    /** Lenient parse of an ISO-ish local datetime; null/blank or unparseable → null. */
    private static LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s.trim());        // e.g. 2026-11-01T19:00
        } catch (RuntimeException ex) {
            return null;                                  // tolerate bad input (store-with-gap)
        }
    }
}
