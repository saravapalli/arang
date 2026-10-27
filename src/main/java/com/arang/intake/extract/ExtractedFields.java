package com.arang.intake.extract;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The **standardized event fields** deciphered from raw producer input (text, and/or image text)
 * by an {@link EventExtractor}. This is the normalized shape every intake path transforms into.
 *
 * <p>Beyond the typed core (title/startsAt/location/eventType), {@code attributes} carries any
 * other useful details found — with meaningful keys (e.g. {@code contact_name}, {@code phone},
 * {@code instagram}, {@code price}, {@code registration_url}, {@code age_groups}) — stored as
 * per-event metadata. {@code rawText} is the full text the model read (useful for image flyers,
 * where no caption exists). Relevance lets garbage/non-event input be filtered.
 */
public record ExtractedFields(
    boolean relevant,
    String irrelevantReason,
    String title,
    LocalDateTime startsAt,
    String location,
    String eventType,
    Double titleConfidence,
    Double startsAtConfidence,
    Double locationConfidence,
    Map<String, String> attributes,
    String rawText
) {
    public ExtractedFields {
        attributes = (attributes == null) ? Map.of() : Map.copyOf(attributes);
    }

    /** Not an event / nothing understood. */
    public static ExtractedFields empty() {
        return new ExtractedFields(false, null, null, null, null, null, null, null, null, Map.of(), null);
    }

    /** Classified as not an event, with a reason. */
    public static ExtractedFields irrelevant(String reason) {
        return new ExtractedFields(false, reason, null, null, null, null, null, null, null, Map.of(), null);
    }

    public boolean hasAnyField() {
        return title != null || startsAt != null || location != null || eventType != null
            || !attributes.isEmpty();
    }
}
