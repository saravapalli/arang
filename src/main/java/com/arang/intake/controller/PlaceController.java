package com.arang.intake.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arang.intake.geo.PlaceSearchService;
import com.arang.intake.geo.PlaceSearchService.Hit;
import com.arang.intake.geo.PlaceSearchService.SearchResult;

/**
 * Town-name search endpoint (ARANG-SDD-P2-1 §4.2, task T6). P2.1 searches by name only.
 */
@RestController
public class PlaceController {

    private static final int MAX_QUERY = 80;

    private final PlaceSearchService search;

    public PlaceController(PlaceSearchService search) {
        this.search = search;
    }

    /**
     * GET /api/places?q=&lt;text&gt;
     * <ul>
     *   <li>200 — results (exact matches first, then up to 8 others); digits-only input returns an empty list with
     *       {@code hint:"type_town_name"} (REQ-P2.1-01, REQ-P2.1-09).</li>
     *   <li>400 {@code q_invalid} — empty after trim, or longer than 80 characters.</li>
     *   <li>503 {@code geo_unavailable} — place data missing or wrong version (REQ-P2.1-18).</li>
     * </ul>
     */
    @GetMapping("/api/places")
    public ResponseEntity<?> places(@RequestParam(name = "q", required = false) String q) {
        String trimmed = q == null ? "" : q.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_QUERY) {
            return ResponseEntity.badRequest().body(Map.of("error", "q_invalid"));
        }
        if (!search.isUp()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "geo_unavailable"));
        }
        SearchResult r = search.search(trimmed);
        if (r.digitsHint()) {
            return ResponseEntity.ok(Map.of("results", List.of(), "hint", "type_town_name"));
        }
        List<Map<String, Object>> results = r.results().stream().map(PlaceController::toJson).toList();
        return ResponseEntity.ok(Map.of("results", results));
    }

    private static Map<String, Object> toJson(Hit h) {
        return Map.of("slug", h.slug(), "name", h.name(), "state", h.state(),
            "live", h.live(), "exact", h.exact());
    }
}
