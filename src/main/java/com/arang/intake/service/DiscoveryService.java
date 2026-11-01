package com.arang.intake.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.arang.intake.dao.EventDao;
import com.arang.intake.dto.DiscoveryResult;
import com.arang.intake.model.Event;

/**
 * Consumer discovery query (ARANG-SDD-P2 §2). Realizes REQ-01..05, REQ-08:
 * published-only, this-week default window, same-postal OR within-radius area match,
 * category OR filter, {@code starts_at ASC} ordering with a result cap.
 */
@Service
public class DiscoveryService {

    /** Thrown when from &gt; to (REQ-04) → the controller maps this to HTTP 400. */
    public static class InvalidWindowException extends RuntimeException {
        public InvalidWindowException() { super("from must be on or before to"); }
    }

    private final EventDao events;
    private final PostalGeoService geo;
    private final NamedParameterJdbcTemplate jdbc;

    @Value("${arang.discovery.radius-miles:15}")
    private int radiusMiles;

    @Value("${arang.discovery.max-results:500}")
    private int maxResults;

    public DiscoveryService(EventDao events, PostalGeoService geo, NamedParameterJdbcTemplate jdbc) {
        this.events = events;
        this.geo = geo;
        this.jdbc = jdbc;
    }

    public int radiusMiles() { return radiusMiles; }
    public int maxResults() { return maxResults; }

    /**
     * Find published events in the window + area, optionally filtered to any of {@code categories}.
     *
     * @param from        window start (inclusive); null → open start
     * @param to          window end (inclusive); null → open end
     * @param postal      query location postal code; null → no area filter
     * @param categories  OR-filter category names; null/empty → no category filter
     * @return matching results, {@code starts_at ASC}, capped at max-results. Truncation is
     *         signalled by the returned list size == cap (the controller sets the envelope flag).
     */
    public List<DiscoveryResult> find(LocalDateTime from, LocalDateTime to,
                                      String postal, List<String> categories) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidWindowException(); // REQ-04
        }

        // Resolve the query centroid for radius matching (REQ-03). Unknown postal → no centroid.
        BigDecimal[] queryCentroid = (postal == null || postal.isBlank())
            ? null
            : geo.deriveCentroid(postal, "US").orElse(null);

        boolean hasCategoryFilter = categories != null && !categories.isEmpty();

        List<DiscoveryResult> out = new ArrayList<>();
        List<Event> all = events.findAll();
        all.sort(Comparator.comparing(Event::getStartsAt,
            Comparator.nullsLast(Comparator.naturalOrder()))); // starts_at ASC

        for (Event e : all) {
            if (!"published".equals(e.getStatus())) continue;           // REQ-01
            if (!inWindow(e.getStartsAt(), from, to)) continue;         // REQ-02
            if (postal != null && !postal.isBlank()
                && !inArea(e, postal, queryCentroid)) continue;         // REQ-03
            if (hasCategoryFilter && !matchesCategory(e.getId(), categories)) continue; // REQ-08
            out.add(toResult(e));
            if (out.size() >= maxResults) break;                        // cap
        }
        return out;
    }

    private boolean inWindow(LocalDateTime startsAt, LocalDateTime from, LocalDateTime to) {
        if (startsAt == null) return false; // undated events are not discoverable in a window
        if (from != null && startsAt.isBefore(from)) return false;
        if (to != null && startsAt.isAfter(to)) return false;
        return true;
    }

    /** Same postal code OR within the radius of the query centroid (REQ-03). */
    private boolean inArea(Event e, String queryPostal, BigDecimal[] queryCentroid) {
        if (queryPostal.equalsIgnoreCase(e.getPostalCode())) return true;
        if (queryCentroid == null) return false;
        if (e.getLatitude() == null || e.getLongitude() == null) return false;
        double miles = haversineMiles(
            queryCentroid[0].doubleValue(), queryCentroid[1].doubleValue(),
            e.getLatitude().doubleValue(), e.getLongitude().doubleValue());
        return miles <= radiusMiles;
    }

    private boolean matchesCategory(long eventId, List<String> categories) {
        List<String> names = jdbc.query(
            "SELECT c.name FROM event_category ec JOIN category c ON c.id = ec.category_id " +
            "WHERE ec.event_id = :id",
            Map.of("id", eventId),
            (rs, n) -> rs.getString("name"));
        return names.stream().anyMatch(categories::contains);
    }

    private DiscoveryResult toResult(Event e) {
        List<String> cats = jdbc.query(
            "SELECT c.name FROM event_category ec JOIN category c ON c.id = ec.category_id " +
            "WHERE ec.event_id = :id",
            Map.of("id", e.getId()),
            (rs, n) -> rs.getString("name"));
        String image = e.getMediaRef() == null ? null : "/" + e.getMediaRef();
        return new DiscoveryResult(
            e.getId(),
            e.getTitle(),
            e.getStartsAt() == null ? null : e.getStartsAt().toString(),
            e.getLocation(),
            e.getPostalCode(),
            e.getLatitude() == null ? null : e.getLatitude().doubleValue(),   // REQ-07: null → list-only
            e.getLongitude() == null ? null : e.getLongitude().doubleValue(),
            cats,
            image,
            null);
    }

    /** Great-circle distance in miles. */
    static double haversineMiles(double lat1, double lon1, double lat2, double lon2) {
        double r = 3958.8; // earth radius in miles
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
