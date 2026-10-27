package com.arang.intake.reply;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.arang.intake.config.WebhookProperties;

/**
 * Builds the WhatsApp reply text for a new draft (REQ-08, AC-005, AC-006). Pure: given a
 * {@link DraftSummary} it returns the message string — no persistence, no network — so the exact
 * wording is unit-testable.
 *
 * <p>Rules:
 * <ul>
 *   <li>Title: shown if present; if below the low-confidence threshold it gets a ⚠️; if absent,
 *       the "couldn't read a title" prompt (AC-006).</li>
 *   <li>Start / location: shown only when present.</li>
 *   <li>URLs: listed when any were captured.</li>
 *   <li>Always ends with the review/edit link containing the edit token (AC-005).</li>
 * </ul>
 */
@Component
public class ReplyComposer {

    private final WebhookProperties props;

    public ReplyComposer(WebhookProperties props) {
        this.props = props;
    }

    public String compose(DraftSummary d) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ Got your event — here's what I captured:\n");

        // Title (AC-006 "couldn't read title" path)
        if (d.title() == null || d.title().isBlank()) {
            sb.append("• Title: ⚠️ couldn't read a title — please add one\n");
        } else if (isLowConfidence(d.titleConfidence())) {
            sb.append("• Title: ").append(d.title()).append("  ⚠️ please check\n");
        } else {
            sb.append("• Title: ").append(d.title()).append('\n');
        }

        if (d.startsAtText() != null && !d.startsAtText().isBlank()) {
            sb.append("• Starts: ").append(d.startsAtText()).append('\n');
        }
        if (d.location() != null && !d.location().isBlank()) {
            sb.append("• Location: ").append(d.location()).append('\n');
        }
        if (!d.urls().isEmpty()) {
            sb.append("• Links:\n");
            for (String url : d.urls()) {
                sb.append("   - ").append(url).append('\n');
            }
        }

        sb.append("\nReview & edit: ").append(reviewLink(d));
        return sb.toString();
    }

    private boolean isLowConfidence(BigDecimal confidence) {
        return confidence != null
            && confidence.compareTo(props.getLowConfidenceThreshold()) < 0;
    }

    private String reviewLink(DraftSummary d) {
        String base = props.getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/review/" + d.eventId() + "?token=" + d.editToken();
    }
}
