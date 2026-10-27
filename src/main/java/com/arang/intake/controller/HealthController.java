package com.arang.intake.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal application health endpoint (SDD T1 "health check").
 *
 * <p>Complements Spring Boot Actuator's {@code /actuator/health}; this is a plain,
 * dependency-light liveness signal useful for the tunnel smoke test in the TTP (TC-028).
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "arang-intake",
            "phase", "P1-skeleton",
            "time", Instant.now().toString()
        );
    }
}
