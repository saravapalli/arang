package com.arang.intake.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.arang.intake.model.Event;
import com.arang.intake.service.IngestService;

/**
 * Direct-entry web form (SDD T8 — REQ-14). Creates a draft of the same shape as a WhatsApp
 * submission ({@code origin_type=user}, no WhatsApp id), then redirects to the token-gated review
 * page so the submitter can review/edit/publish exactly as the WhatsApp flow does.
 */
@Controller
public class FormController {

    private final IngestService ingestService;

    public FormController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @GetMapping("/submit")
    public String form() {
        return "form";
    }

    @PostMapping("/submit")
    public String create(@RequestParam(required = false) String title,
                         @RequestParam(required = false) String location,
                         @RequestParam(required = false) String postalCode,
                         @RequestParam(required = false) String details) {
        Event draft = ingestService.ingestForm(title, location, postalCode, details);
        // redirect to the review page with the capability token (same surface as WhatsApp)
        return "redirect:/review/" + draft.getId() + "?token=" + draft.getEditToken();
    }
}
