package com.arang.intake.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.arang.intake.model.Event;
import com.arang.intake.service.ReviewService;

/**
 * Token-gated review/edit page (SDD T5 — REQ-09, REQ-10). Server-rendered Thymeleaf (DD-4).
 * Unauthorized access (missing/invalid/expired token, unknown event) → 404 so the page does not
 * reveal whether an event exists.
 */
@Controller
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/review/{id}")
    public String review(@PathVariable Long id,
                         @RequestParam(required = false) String token,
                         Model model) {
        Event e = reviewService.authorize(id, token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("event", e);
        model.addAttribute("token", token);
        return "review";
    }

    @PostMapping("/review/{id}")
    public String save(@PathVariable Long id,
                       @RequestParam(required = false) String token,
                       @RequestParam(required = false) String title,
                       @RequestParam(required = false) String startsAt,
                       @RequestParam(required = false) String location,
                       @RequestParam(required = false) String postalCode,
                       Model model) {
        boolean ok = reviewService.saveEdits(id, token, title, startsAt, location, postalCode);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // re-render with saved values + a confirmation
        Event e = reviewService.authorize(id, token).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("event", e);
        model.addAttribute("token", token);
        model.addAttribute("saved", true);
        return "review";
    }

    @PostMapping("/review/{id}/submit")
    public String submit(@PathVariable Long id,
                         @RequestParam(required = false) String token,
                         Model model) {
        ReviewService.SubmitResult result = reviewService.submit(id, token);
        if (result == ReviewService.SubmitResult.UNAUTHORIZED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Event e = reviewService.authorize(id, token).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("event", e);
        model.addAttribute("token", token);
        if (result == ReviewService.SubmitResult.MISSING_POSTAL) {
            model.addAttribute("error", "A postal code is required to publish.");
        } else {
            model.addAttribute("published", true);
        }
        return "review";
    }

    @PostMapping("/review/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String token,
                         Model model) {
        boolean ok = reviewService.delete(id, token);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "deleted";
    }
}
