package com.arang.intake.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.arang.intake.config.WebhookProperties;
import com.arang.intake.dao.EventDao;
import com.arang.intake.dao.EventMetadataDao;
import com.arang.intake.model.Event;
import com.arang.intake.reply.DraftSummary;
import com.arang.intake.reply.ReplyComposer;
import com.arang.intake.service.IngestResult;
import com.arang.intake.service.IngestService;

/**
 * Producer Console — a browser UI for sending a message to Arang exactly like a WhatsApp producer
 * would. The typed message runs through the real ingest pipeline ({@link IngestService#ingestText})
 * and the composed confirmation reply + review link are returned so the console can show them
 * inline. This is a local operator surface (network-restricted, same posture as the review page).
 */
@Controller
public class ProducerConsoleController {

    private final IngestService ingestService;
    private final ReplyComposer replyComposer;
    private final WebhookProperties props;
    private final EventDao events;
    private final EventMetadataDao metadata;

    public ProducerConsoleController(IngestService ingestService, ReplyComposer replyComposer,
                                     WebhookProperties props, EventDao events,
                                     EventMetadataDao metadata) {
        this.ingestService = ingestService;
        this.replyComposer = replyComposer;
        this.props = props;
        this.events = events;
        this.metadata = metadata;
    }

    /** The chat-style console page. */
    @GetMapping("/")
    public String console() {
        return "console";
    }

    /** Ingest a typed message; return the reply text + review link (JSON) for the console. */
    @PostMapping("/api/producer/message")
    @ResponseBody
    public Map<String, Object> send(@RequestBody Map<String, String> body) {
        String message = body == null ? null : body.get("message");
        String phone = body != null && body.get("phone") != null && !body.get("phone").isBlank()
            ? body.get("phone") : "web-console";
        String name = body != null ? body.getOrDefault("name", "Console") : "Console";

        if (message == null || message.isBlank()) {
            return Map.of("ok", false, "error", "Please type a message.");
        }

        IngestResult result = ingestService.ingestText(message, phone, name);
        DraftSummary s = result.summary();
        String reply = replyComposer.compose(s);
        String reviewPath = "/review/" + s.eventId() + "?token=" + s.editToken();
        String reviewUrl = props.getPublicBaseUrl() + reviewPath;

        return Map.of(
            "ok", true,
            "reply", reply,
            "eventId", s.eventId(),
            "reviewPath", reviewPath,   // relative — works in-browser regardless of tunnel config
            "reviewUrl", reviewUrl      // absolute (uses public-base-url) for sharing
        );
    }

    /**
     * Upload a flyer image. Creates an image draft immediately and runs multimodal LLM extraction
     * asynchronously (Gemini reads the flyer). Returns the review link; the console polls the
     * status endpoint for the deciphered fields.
     */
    @PostMapping("/api/producer/image")
    @ResponseBody
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "caption", required = false) String caption) {
        if (file == null || file.isEmpty()) {
            return Map.of("ok", false, "error", "Please choose an image.");
        }
        try {
            Event draft = ingestService.ingestImageBytes(
                file.getBytes(), file.getContentType(), caption, "web-console", "Console");
            String reviewPath = "/review/" + draft.getId() + "?token=" + draft.getEditToken();
            return Map.of(
                "ok", true,
                "eventId", draft.getId(),
                "reviewPath", reviewPath,
                "reviewUrl", props.getPublicBaseUrl() + reviewPath,
                "message", "📷 Got your flyer — reading it now…"
            );
        } catch (java.io.IOException e) {
            return Map.of("ok", false, "error", "Could not read the uploaded image.");
        }
    }

    /**
     * Enrichment status for an event — the console polls this after sending so it can show the
     * LLM-deciphered fields (title/start/location) once the async extraction completes, or report
     * that the message wasn't understood as an event.
     */
    @GetMapping("/api/producer/message/{eventId}/status")
    @ResponseBody
    public Map<String, Object> status(@PathVariable Long eventId) {
        var found = events.findById(eventId);
        if (found.isEmpty()) {
            return Map.of("ok", false, "error", "not found");
        }
        var e = found.get();
        boolean enriched = e.getTitle() != null || e.getStartsAt() != null || e.getLocation() != null;
        // A garbage/non-event message is flagged in metadata by the enrichment step.
        String notEvent = metadata.findByEventId(eventId).stream()
            .filter(m -> "llm_relevance".equals(m.getKey()))
            .map(m -> m.getValue())
            .findFirst().orElse(null);

        var out = new java.util.HashMap<String, Object>();
        out.put("ok", true);
        out.put("enriched", enriched);
        out.put("notEvent", notEvent);                 // non-null if classified not-an-event
        out.put("title", e.getTitle());
        out.put("startsAt", e.getStartsAt() == null ? null : e.getStartsAt().toString());
        out.put("location", e.getLocation());
        return out;
    }
}
