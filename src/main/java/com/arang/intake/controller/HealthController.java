package com.arang.intake.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arang.intake.geo.GeoPlaceSource;

/**
 * Minimal application health endpoint (SDD T1 "health check"; extended in ARANG-SDD-P2-1 §4.7).
 *
 * <p>Complements Spring Boot Actuator's {@code /actuator/health}. The top-level {@code status} stays {@code UP} when
 * place data is unavailable, because event pages work without it. The {@code geo} section reports the place file:
 * status, data date, schema version, and a reason from a fixed list. It never contains a file path or exception text,
 * because this endpoint is public.
 */
@RestController
public class HealthController {

    private final GeoPlaceSource geo;

    public HealthController(GeoPlaceSource geo) {
        this.geo = geo;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> geoInfo = new LinkedHashMap<>();
        geoInfo.put("status", geo.status().name());
        geo.dataDate().ifPresent(d -> geoInfo.put("dataDate", d));
        geo.schemaVersion().ifPresent(v -> geoInfo.put("schemaVersion", v));
        geo.reason().ifPresent(r -> geoInfo.put("reason", r.name()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "arang-intake");
        body.put("phase", "P2.1");
        body.put("time", Instant.now().toString());
        body.put("geo", geoInfo);
        return body;
    }
}
