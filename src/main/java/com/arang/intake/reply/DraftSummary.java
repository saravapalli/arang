package com.arang.intake.reply;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable summary of a just-created draft, enough for {@link ReplyComposer} to build the reply
 * (REQ-08). Kept separate from the JPA entity so reply composition is a pure, easily-tested
 * function with no persistence coupling.
 *
 * @param eventId        the draft's id (for the review link)
 * @param editToken      the capability token (for the review link)
 * @param title          extracted title, or null if none was read
 * @param titleConfidence confidence of the title, or null if not extracted
 * @param startsAtText   human text for start time if present, else null
 * @param location       location if present, else null
 * @param urls           extracted URL values (may be empty)
 */
public record DraftSummary(
    long eventId,
    String editToken,
    String title,
    BigDecimal titleConfidence,
    String startsAtText,
    String location,
    List<String> urls
) {
    public DraftSummary {
        urls = (urls == null) ? List.of() : List.copyOf(urls);
    }
}
