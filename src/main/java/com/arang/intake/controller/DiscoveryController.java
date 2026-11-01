package com.arang.intake.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arang.intake.dto.DiscoveryEnvelope;
import com.arang.intake.dto.DiscoveryResult;
import com.arang.intake.service.DiscoveryService;
import com.arang.intake.service.WeekWindow;

/**
 * Consumer discovery JSON endpoints (ARANG-SDD-P2 §2, REQ-01..09, REQ-19).
 * Serves the React SPA same-origin (DD-P2-6).
 */
@RestController
public class DiscoveryController {

    private final DiscoveryService discovery;
    private final NamedParameterJdbcTemplate jdbc;
    private final com.arang.intake.service.ConsumerIdentityResolver identity;
    private final com.arang.intake.service.InterestService interests;
    private final com.arang.intake.dao.EventDao events;
    private final com.arang.intake.dao.EventMetadataDao metadata;

    @Value("${arang.discovery.timezone:America/New_York}")
    private String zoneId;

    public DiscoveryController(DiscoveryService discovery, NamedParameterJdbcTemplate jdbc,
                               com.arang.intake.service.ConsumerIdentityResolver identity,
                               com.arang.intake.service.InterestService interests,
                               com.arang.intake.dao.EventDao events,
                               com.arang.intake.dao.EventMetadataDao metadata) {
        this.discovery = discovery;
        this.jdbc = jdbc;
        this.identity = identity;
        this.interests = interests;
        this.events = events;
        this.metadata = metadata;
    }

    /**
     * GET /api/discovery?from&to&location&categories
     * Defaults to this week (REQ-02) when no window is given. location = postal code (DD-P2-3).
     * When no explicit categories are passed, the recognized consumer's saved interests are used
     * as the filter (REQ-13). A recognized consumer with NO interests (or an unrecognized visitor)
     * gets the unfiltered in-window/in-area set with {@code interestFilterMatchedNothing=true} (REQ-14).
     */
    @GetMapping("/api/discovery")
    public DiscoveryEnvelope discover(
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(required = false) String location,
        @RequestParam(required = false) String categories,
        jakarta.servlet.http.HttpServletRequest req) {

        ZoneId zone = ZoneId.of(zoneId);
        WeekWindow week = WeekWindow.thisWeek(zone);
        LocalDateTime fromTs = from != null ? LocalDate.parse(from).atStartOfDay() : week.from();
        LocalDateTime toTs = to != null ? LocalDate.parse(to).atTime(23, 59, 59) : week.to();

        List<String> explicitCats = (categories == null || categories.isBlank())
            ? List.of()
            : Arrays.stream(categories.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

        // REQ-13/14: with no explicit filter, fall back to the recognized consumer's interests.
        boolean interestFilterMatchedNothing = false;
        List<String> effectiveCats = explicitCats;
        if (explicitCats.isEmpty()) {
            String ref = cookie(req);
            List<String> saved = (ref == null)
                ? List.of()
                : identity.recognize(null, ref).map(u -> interests.get(u.getId())).orElse(List.of());
            if (saved.isEmpty()) {
                interestFilterMatchedNothing = true; // unfiltered + flag (REQ-14)
            } else {
                effectiveCats = saved; // interest pre-filter (REQ-13)
            }
        }

        List<DiscoveryResult> results = discovery.find(fromTs, toTs, location, effectiveCats);

        boolean truncated = results.size() >= discovery.maxResults();
        return new DiscoveryEnvelope(
            new DiscoveryEnvelope.Window(fromTs.toString(), toTs.toString()),
            new DiscoveryEnvelope.Area(location, discovery.radiusMiles()),
            truncated,
            interestFilterMatchedNothing,
            results);
    }

    private static String cookie(jakarta.servlet.http.HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        for (jakarta.servlet.http.Cookie c : req.getCookies()) {
            if ("arang_uid".equals(c.getName())) return c.getValue();
        }
        return null;
    }

    /** GET /api/categories — the curated vocabulary (REQ-19; avoids client-side drift, gate L3). */
    @GetMapping("/api/categories")
    public List<String> categories() {
        return jdbc.getJdbcOperations().query(
            "SELECT name FROM category ORDER BY name", (rs, n) -> rs.getString("name"));
    }

    /** GET /api/events/{id} — full detail for a PUBLISHED event; draft/unknown → 404 (REQ-09). */
    @GetMapping("/api/events/{id}")
    public com.arang.intake.dto.EventDetail eventDetail(@org.springframework.web.bind.annotation.PathVariable long id) {
        com.arang.intake.model.Event e = events.findById(id)
            .filter(ev -> "published".equals(ev.getStatus()))
            .orElseThrow(NotFoundException::new); // draft or unknown → 404

        List<String> cats = jdbc.query(
            "SELECT c.name FROM event_category ec JOIN category c ON c.id=ec.category_id WHERE ec.event_id=:id",
            Map.of("id", id), (rs, n) -> rs.getString("name"));
        List<String> tags = jdbc.query(
            "SELECT t.name FROM event_tag et JOIN tag t ON t.id=et.tag_id WHERE et.event_id=:id",
            Map.of("id", id), (rs, n) -> rs.getString("name"));
        java.util.Map<String, String> attrs = new java.util.LinkedHashMap<>();
        metadata.findByEventId(id).forEach(m -> {
            if (!"system".equals(m.getSource())) attrs.putIfAbsent(m.getKey(), m.getValue());
        });

        return new com.arang.intake.dto.EventDetail(
            e.getId(), e.getTitle(),
            e.getStartsAt() == null ? null : e.getStartsAt().toString(),
            e.getEndsAt() == null ? null : e.getEndsAt().toString(),
            e.getLocation(), e.getPostalCode(),
            e.getLatitude() == null ? null : e.getLatitude().doubleValue(),
            e.getLongitude() == null ? null : e.getLongitude().doubleValue(),
            cats, tags,
            e.getMediaRef() == null ? null : "/" + e.getMediaRef(),
            attrs);
    }

    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NOT_FOUND)
    static class NotFoundException extends RuntimeException {}

    /** REQ-04: from > to → HTTP 400 with a named error body. */
    @ExceptionHandler(DiscoveryService.InvalidWindowException.class)
    public ResponseEntity<Map<String, String>> onInvalidWindow(DiscoveryService.InvalidWindowException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "invalid_window", "message", ex.getMessage()));
    }
}
