package com.arang.intake.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.arang.intake.service.WebhookService;
import tools.jackson.databind.JsonNode;

/**
 * WhatsApp Cloud API webhook (SDD task T2 — REQ-01, REQ-07).
 *
 * <ul>
 *   <li>{@code GET /webhook} — Meta's verification handshake: echo {@code hub.challenge} iff
 *       {@code hub.verify_token} matches the configured token, else 403.</li>
 *   <li>{@code POST /webhook} — receive message callbacks; record message ids in the idempotency
 *       ledger and return 200 fast. Event creation is T3.</li>
 * </ul>
 */
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        String echoed = webhookService.verifySubscription(mode, token, challenge);
        if (echoed == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(echoed);
    }

    @PostMapping
    public ResponseEntity<Void> receive(@RequestBody(required = false) JsonNode payload) {
        // Idempotent record; tolerant of status-only/empty payloads. Always 200 fast so Meta
        // does not retry (REQ-01). Processing/event-creation happens in T3.
        webhookService.receive(payload);
        return ResponseEntity.ok().build();
    }
}
