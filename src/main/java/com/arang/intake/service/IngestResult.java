package com.arang.intake.service;

import com.arang.intake.reply.DraftSummary;

/**
 * Outcome of ingesting one message, returned so the caller can send the reply <em>after</em> the
 * draft transaction commits (REQ-08, send-after-commit). Carries the recipient and a pure
 * {@link DraftSummary} for reply composition.
 *
 * @param recipientPhone who to reply to (may be null if the sender phone was absent → no reply)
 * @param summary        draft summary for {@code ReplyComposer}
 */
public record IngestResult(String recipientPhone, DraftSummary summary) {
}
