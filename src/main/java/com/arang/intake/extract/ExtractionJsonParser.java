package com.arang.intake.extract;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared logic for turning an LLM's text reply into {@link ExtractedFields}: the extraction
 * prompt and a tolerant JSON parser (handles code fences / surrounding prose, classifies
 * relevance, maps fields + confidences). Reused by every LLM provider (Ollama, Gemini) so the
 * prompt and parsing stay identical regardless of backend.
 */
public final class ExtractionJsonParser {

    private static final Logger log = LoggerFactory.getLogger(ExtractionJsonParser.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    /** The extraction + garbage-classification instruction (provider-agnostic, date-agnostic base). */
    public static final String SYSTEM_PROMPT = """
        A person sent a message (possibly with text read from an attached image) trying to list an
        event. First decide whether it actually describes a real event. Then extract its fields.
        Reply with ONLY a JSON object (no prose, no code fences):
          "relevant": true|false,            // false for greetings, spam, questions, nonsense
          "reason": string|null,             // if not relevant, a short why
          "title": string|null,
          "starts_at": ISO-8601 local date-time (YYYY-MM-DDTHH:MM) or null,
          "location": string|null,
          "event_type": one of concert/offer/workshop/festival/class/other or null,
          "confidence": { "title": 0..1, "starts_at": 0..1, "location": 0..1 },
          "attributes": { },                 // any OTHER useful details found, as key/value pairs
          "raw_text": string|null            // the full text you read (esp. from an image flyer)
        For "attributes", use meaningful lower_snake_case keys and capture everything else of value:
        e.g. contact_name, phone, email, instagram, website, registration_url, ticket_url, price,
        age_groups, organizer, cost, rsvp. Omit keys you don't find; never invent values.
        If relevant is false, set the other fields to null/empty. Use null when a field is not
        stated; do not invent values.
        """;

    /**
     * The system prompt anchored to "now" so the model resolves RELATIVE dates correctly
     * ("this Friday", "Sunday", "tomorrow") instead of guessing a training-era date. Call this
     * per request rather than using the static constant directly.
     */
    public static String systemPrompt(java.time.LocalDate today) {
        String dow = today.getDayOfWeek().getDisplayName(
            java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
        return SYSTEM_PROMPT
            + "\nFor dates: today is " + dow + ", " + today + ". Resolve relative dates"
            + " (e.g. \"today\", \"tomorrow\", \"this Friday\", \"Sunday\", \"next week\")"
            + " against today, choosing the next upcoming occurrence. Never use a past year.";
    }

    private ExtractionJsonParser() {
    }

    /** Parse the model's content string into fields. Never throws — returns empty on any problem. */
    public static ExtractedFields parse(String content) {
        if (content == null || content.isBlank()) {
            return ExtractedFields.empty();
        }
        try {
            Matcher m = JSON_OBJECT.matcher(content);
            if (!m.find()) {
                return ExtractedFields.empty();
            }
            JsonNode n = JSON.readTree(m.group());

            JsonNode rel = n.get("relevant");
            boolean relevant = (rel == null || rel.isNull()) ? true : rel.asBoolean();
            if (!relevant) {
                return ExtractedFields.irrelevant(textOrNull(n, "reason"));
            }

            JsonNode conf = n.path("confidence");
            return new ExtractedFields(
                true,
                null,
                textOrNull(n, "title"),
                parseDateTime(textOrNull(n, "starts_at")),
                textOrNull(n, "location"),
                textOrNull(n, "event_type"),
                confOrNull(conf, "title"),
                confOrNull(conf, "starts_at"),
                confOrNull(conf, "location"),
                parseAttributes(n.get("attributes")),
                textOrNull(n, "raw_text"));
        } catch (Exception e) {
            log.warn("LLM JSON parse failed: {}", e.toString());
            return ExtractedFields.empty();
        }
    }

    /** Read the "attributes" object into a map of meaningful-key → value (skips blanks/nulls). */
    private static java.util.Map<String, String> parseAttributes(JsonNode node) {
        if (node == null || !node.isObject()) {
            return java.util.Map.of();
        }
        var out = new java.util.LinkedHashMap<String, String>();
        node.properties().forEach(entry -> {
            JsonNode v = entry.getValue();
            if (v != null && !v.isNull()) {
                String val = v.isValueNode() ? v.asString().trim() : v.toString();
                if (!val.isEmpty() && !"null".equalsIgnoreCase(val)) {
                    out.put(entry.getKey(), val);
                }
            }
        });
        return out;
    }

    private static String textOrNull(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return null;
        }
        String s = v.asString().trim();
        return (s.isEmpty() || "null".equalsIgnoreCase(s)) ? null : s;
    }

    private static Double confOrNull(JsonNode conf, String field) {
        JsonNode v = conf.get(field);
        return (v == null || v.isNull() || !v.isNumber()) ? null : v.asDouble();
    }

    private static LocalDateTime parseDateTime(String s) {
        if (s == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignore) {
            try {
                return LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
