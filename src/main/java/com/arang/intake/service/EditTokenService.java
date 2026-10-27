package com.arang.intake.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.stereotype.Component;

/**
 * Generates the per-event edit token (SDD; REQ-08/09/10). A URL-safe, unguessable 256-bit secret
 * (32 random bytes → 43 Base64url chars, matching the schema's {@code CHAR(43)}), with a 24h
 * expiry aligned to WhatsApp's free messaging window (PRD OQ-P1-2).
 */
@Component
public class EditTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    public static final int TOKEN_TTL_HOURS = 24;

    /** 32 random bytes → 43-char URL-safe Base64 string (no padding). */
    public String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    public LocalDateTime expiryFromNow() {
        return LocalDateTime.now().plusHours(TOKEN_TTL_HOURS);
    }
}
