package com.arang.intake.controller;

import java.time.Clock;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arang.intake.service.DiscoveryService;
import com.arang.intake.service.WeekWindow;
import com.arang.intake.town.Town;
import com.arang.intake.town.TownRegistry;

/**
 * Nearby live towns that have events, for the "nothing on" state (ARANG-SDD-P2-1 §4.4, task T13, REQ-P2.1-08):
 * up to 3 other live towns within the near-me limit with ≥1 published event in the window, nearest first.
 *
 * <p>Only the default (this-week) window is cached, for 60 seconds, because {@code DiscoveryService.find} scans all
 * events on each call. Other windows are computed on demand and not cached, so callers cannot fill memory.
 */
@RestController
public class NearbyController {

    private static final int MAX_NEARBY = 3;
    private static final int MAX_WINDOW_DAYS = 31;
    private static final double EARTH_MILES = 3958.8;

    private final TownRegistry towns;
    private final DiscoveryService discovery;
    private final Clock clock;

    @Value("${arang.site.near-me-miles:100}")
    private double nearMeMiles;

    @Value("${arang.discovery.timezone:America/New_York}")
    private String zoneId;

    @Value("${arang.site.page-cache-seconds:60}")
    private long cacheSeconds;

    // Cache of the default-window result per town slug: value = (expiry millis, JSON-ready list).
    private final Map<String, Cached> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public NearbyController(TownRegistry towns, DiscoveryService discovery, Clock clock) {
        this.towns = towns;
        this.discovery = discovery;
        this.clock = clock;
    }

    private record Cached(long expiresAt, List<Map<String, Object>> towns) {}

    @GetMapping("/api/towns/{slug}/nearby")
    public ResponseEntity<?> nearby(@PathVariable String slug,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to) {
        Optional<Town> town = towns.bySlug(slug).filter(Town::isLive);
        if (town.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ZoneId zone = ZoneId.of(zoneId);
        WeekWindow week = WeekWindow.thisWeek(zone);
        boolean defaultWindow = (from == null && to == null);

        java.time.LocalDateTime fromTs;
        java.time.LocalDateTime toTs;
        if (defaultWindow) {
            fromTs = week.from();
            toTs = week.to();
        } else {
            try {
                fromTs = from != null ? java.time.LocalDate.parse(from).atStartOfDay() : week.from();
                toTs = to != null ? java.time.LocalDate.parse(to).atTime(23, 59, 59) : week.to();
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "bad_window"));
            }
            if (fromTs.isAfter(toTs) || fromTs.plusDays(MAX_WINDOW_DAYS).isBefore(toTs)) {
                return ResponseEntity.badRequest().body(Map.of("error", "bad_window"));
            }
        }

        if (defaultWindow) {
            Cached c = cache.get(slug);
            if (c != null && c.expiresAt() > clock.millis()) {
                return ResponseEntity.ok(Map.of("towns", c.towns()));
            }
        }

        List<Map<String, Object>> result = compute(town.get(), fromTs, toTs);
        if (defaultWindow) {
            cache.put(slug, new Cached(clock.millis() + cacheSeconds * 1000, result));
        }
        return ResponseEntity.ok(Map.of("towns", result));
    }

    private List<Map<String, Object>> compute(Town origin,
                                              java.time.LocalDateTime fromTs, java.time.LocalDateTime toTs) {
        List<Map<String, Object>> out = new ArrayList<>();
        List<Town> candidates = new ArrayList<>(towns.liveTowns());
        candidates.sort(Comparator.comparingDouble(t -> miles(origin, t)));
        for (Town t : candidates) {
            if (t.slug().equals(origin.slug())) {
                continue;
            }
            double d = miles(origin, t);
            if (d > nearMeMiles) {
                continue;
            }
            int count;
            try {
                count = discovery.find(fromTs, toTs, t.postalCode(), List.of()).size();
            } catch (RuntimeException e) {
                count = 0;
            }
            if (count > 0) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("slug", t.slug());
                m.put("name", t.name());
                m.put("state", t.state());
                m.put("distanceMiles", Math.round(d * 10) / 10.0);
                m.put("eventCount", count);
                out.add(m);
                if (out.size() >= MAX_NEARBY) {
                    break;
                }
            }
        }
        return out;
    }

    private static double miles(Town a, Town b) {
        double rLat1 = Math.toRadians(a.latitude());
        double rLat2 = Math.toRadians(b.latitude());
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLng = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return EARTH_MILES * 2 * Math.asin(Math.sqrt(h));
    }
}
