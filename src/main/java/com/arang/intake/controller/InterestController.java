package com.arang.intake.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.arang.intake.model.User;
import com.arang.intake.service.ConsumerIdentityResolver;
import com.arang.intake.service.InterestService;

/**
 * Consumer interests JSON endpoints (ARANG-SDD-P2 §2, REQ-10..12).
 * Web identity is a persistent cookie ({@code arang_uid}) → a lightweight {@code user} row via
 * {@link ConsumerIdentityResolver} (DD-P2-2). GET reads; PUT saves (creating the identity row).
 */
@RestController
public class InterestController {

    private static final String COOKIE = "arang_uid";

    private final ConsumerIdentityResolver identity;
    private final InterestService interests;

    public InterestController(ConsumerIdentityResolver identity, InterestService interests) {
        this.identity = identity;
        this.interests = interests;
    }

    /** GET /api/interests — the recognized consumer's saved interests (empty if unrecognized). */
    @GetMapping("/api/interests")
    public Map<String, Object> get(HttpServletRequest req) {
        String ref = cookie(req);
        List<String> cats = (ref == null)
            ? List.of()
            : identity.recognize(null, ref).map(u -> interests.get(u.getId())).orElse(List.of());
        return Map.of("categories", cats);
    }

    /** PUT /api/interests {categories:[...]} — save; sets the identity cookie if absent. */
    @PutMapping("/api/interests")
    public ResponseEntity<Map<String, Object>> put(@RequestBody Map<String, List<String>> body,
                                                   HttpServletRequest req, HttpServletResponse res) {
        String ref = cookie(req);
        if (ref == null) {
            ref = UUID.randomUUID().toString();
            Cookie c = new Cookie(COOKIE, ref);
            c.setPath("/");
            c.setMaxAge(60 * 60 * 24 * 365); // 1 year
            c.setHttpOnly(true);
            res.addCookie(c);
        }
        User u = identity.resolveWeb(ref); // creates the row on first save (REQ-12)
        List<String> categories = body.getOrDefault("categories", List.of());
        interests.save(u.getId(), categories); // validates vocabulary (REQ-11)
        return ResponseEntity.ok(Map.of("categories", interests.get(u.getId())));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(InterestService.UnknownCategoryException.class)
    public ResponseEntity<Map<String, String>> onUnknownCategory(InterestService.UnknownCategoryException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "unknown_category", "message", ex.getMessage()));
    }

    private static String cookie(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) {
            if (COOKIE.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
