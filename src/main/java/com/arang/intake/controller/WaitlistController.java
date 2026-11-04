package com.arang.intake.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.arang.intake.waitlist.ClientKeyResolver;
import com.arang.intake.waitlist.RequestThrottle;
import com.arang.intake.waitlist.WaitlistService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Waitlist capture endpoint (ARANG-SDD-P2-1 §4.3, tasks T10/T11). Throttled per client; the same success body is
 * returned for a new record and a repeat, so no response reveals whether an email is already stored (REQ-P2.1-12).
 * A catch-all handler keeps any exception — whose message could contain the email — out of the default error log.
 */
@RestController
public class WaitlistController {

    private static final Logger log = LoggerFactory.getLogger(WaitlistController.class);
    private static final int RETRY_AFTER_SECONDS = 60;

    private final WaitlistService service;
    private final RequestThrottle throttle;
    private final ClientKeyResolver clientKey;

    public WaitlistController(WaitlistService service, RequestThrottle throttle, ClientKeyResolver clientKey) {
        this.service = service;
        this.throttle = throttle;
        this.clientKey = clientKey;
    }

    @PostMapping("/api/waitlist")
    public ResponseEntity<?> join(@RequestBody(required = false) Map<String, String> body,
                                  HttpServletRequest req) {
        if (!throttle.allow(clientKey.keyFor(req))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(RETRY_AFTER_SECONDS))
                .body(Map.of("retryAfterSeconds", RETRY_AFTER_SECONDS));
        }
        String place = body == null ? null : body.get("place");
        String email = body == null ? null : body.get("email");
        try {
            WaitlistService.Result r = service.save(place, email);
            return switch (r) {
                case SAVED -> ResponseEntity.ok(Map.of("status", "saved"));
                case INVALID_EMAIL -> ResponseEntity.badRequest().body(Map.of("errors", Map.of("email", "invalid")));
                case INVALID_PLACE -> ResponseEntity.badRequest().body(Map.of("errors", Map.of("place", "invalid")));
                case NOT_SAVED -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "not_saved"));
            };
        } catch (RuntimeException e) {
            // Never let the exception reach the default error logger: its message may contain the email.
            log.error("Waitlist request failed: {}", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "not_saved"));
        }
    }
}
