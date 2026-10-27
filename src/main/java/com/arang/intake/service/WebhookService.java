package com.arang.intake.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.arang.intake.config.WebhookProperties;
import com.arang.intake.reply.ReplyComposer;
import com.arang.intake.whatsapp.WhatsAppClient;
import tools.jackson.databind.JsonNode;

/**
 * Webhook plumbing (REQ-01 verify + receive, REQ-07 idempotency) + T3 ingest dispatch + T4 reply.
 *
 * <p>Each new text message creates a draft (committed via the transactional {@link #persist}),
 * and the reply is composed and sent <em>after</em> that commit (REQ-08, send-after-commit) so a
 * failed send never rolls back a captured draft. A send failure is logged, not fatal.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookProperties properties;
    private final IngestService ingestService;
    private final ReplyComposer replyComposer;
    private final WhatsAppClient whatsAppClient;

    public WebhookService(WebhookProperties properties, IngestService ingestService,
                          ReplyComposer replyComposer, WhatsAppClient whatsAppClient) {
        this.properties = properties;
        this.ingestService = ingestService;
        this.replyComposer = replyComposer;
        this.whatsAppClient = whatsAppClient;
    }

    /** REQ-01 (GET): echo the challenge iff mode=subscribe and the verify token matches. */
    public String verifySubscription(String mode, String token, String challenge) {
        String expected = properties.getVerifyToken();
        boolean ok = "subscribe".equals(mode)
            && expected != null && !expected.isEmpty()
            && expected.equals(token);
        if (!ok) {
            log.warn("Webhook verification rejected (mode={})", mode);
            return null;
        }
        return challenge;
    }

    /**
     * REQ-01 (POST) + REQ-07 + T3 ingest + T4 reply. Not transactional itself: per-message DB work
     * is committed by {@link #persist}; replies are sent afterwards. Returns the number of newly
     * recorded messages. Tolerates malformed/empty payloads (returns 0) so the caller 200s fast.
     */
    public int receive(JsonNode payload) {
        List<InboundMessage> messages = parseMessages(payload);
        if (messages.isEmpty()) {
            log.debug("Webhook POST with no messages (status-only callback or empty)");
            return 0;
        }
        int recorded = 0;
        for (InboundMessage msg : messages) {
            // Transactional per-message DB work on a separate bean (so @Transactional applies).
            IngestResult result = ingestService.processMessage(
                msg.id(), msg.type(), msg.text(), msg.mediaId(), msg.fromPhone(), msg.fromName());
            if (result == null) {
                continue; // duplicate, or non-ingestable type recorded only
            }
            recorded++;
            sendReply(result); // after commit; failure logged, not fatal
        }
        return recorded;
    }

    private void sendReply(IngestResult result) {
        if (result.recipientPhone() == null || result.recipientPhone().isBlank()) {
            log.info("No recipient phone on event {} — skipping reply", result.summary().eventId());
            return;
        }
        try {
            String body = replyComposer.compose(result.summary());
            whatsAppClient.sendText(result.recipientPhone(), body);
        } catch (RuntimeException e) {
            // REQ-08 send-after-commit: the draft is already saved; a failed reply is not fatal.
            log.warn("Failed to send WhatsApp reply for event {}: {}",
                result.summary().eventId(), e.toString());
        }
    }

    /** Minimal, tolerant parse of entry[].changes[].value.{messages,contacts}. */
    private List<InboundMessage> parseMessages(JsonNode payload) {
        List<InboundMessage> out = new ArrayList<>();
        if (payload == null) {
            return out;
        }
        for (JsonNode entry : arrayOrEmpty(payload.get("entry"))) {
            for (JsonNode change : arrayOrEmpty(entry.get("changes"))) {
                JsonNode value = change.get("value");
                if (value == null) {
                    continue;
                }
                String contactName = firstContactName(value.get("contacts"));
                for (JsonNode m : arrayOrEmpty(value.get("messages"))) {
                    String id = text(m.get("id"));
                    if (id == null || id.isBlank()) {
                        continue;
                    }
                    String type = text(m.get("type"));
                    String from = text(m.get("from"));
                    String body = null;
                    JsonNode textNode = m.get("text");
                    if (textNode != null) {
                        body = text(textNode.get("body"));
                    }
                    String mediaId = null;
                    JsonNode imageNode = m.get("image");
                    if (imageNode != null) {
                        mediaId = text(imageNode.get("id"));
                        if (body == null) {
                            body = text(imageNode.get("caption")); // image caption as raw text
                        }
                    }
                    out.add(new InboundMessage(id, type, body, mediaId, from, contactName));
                }
            }
        }
        return out;
    }

    private static String firstContactName(JsonNode contacts) {
        for (JsonNode c : arrayOrEmpty(contacts)) {
            JsonNode profile = c.get("profile");
            if (profile != null) {
                return text(profile.get("name"));
            }
        }
        return null;
    }

    private static String text(JsonNode node) {
        return (node != null && node.isTextual()) ? node.asText() : null;
    }

    private static Iterable<JsonNode> arrayOrEmpty(JsonNode node) {
        return (node != null && node.isArray()) ? node : List.of();
    }

    /** Parsed inbound message fields the ingest flow needs. */
    record InboundMessage(String id, String type, String text, String mediaId,
                          String fromPhone, String fromName) {
    }
}
