package com.arang.intake.reply;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.arang.intake.config.WebhookProperties;

/** Pure unit tests for the reply text (REQ-08, AC-005, AC-006) — no Spring, no network. */
class ReplyComposerTest {

    private ReplyComposer composer() {
        WebhookProperties props = new WebhookProperties();
        props.setPublicBaseUrl("https://tunnel.example");
        props.setLowConfidenceThreshold(new BigDecimal("0.5"));
        return new ReplyComposer(props);
    }

    @Test
    void absentTitleShowsCouldntReadPrompt() {
        String reply = composer().compose(new DraftSummary(
            7L, "tok123", null, null, null, null, List.of()));
        assertThat(reply).contains("couldn't read a title");       // AC-006
        assertThat(reply).contains("Review & edit: https://tunnel.example/review/7?token=tok123"); // AC-005
    }

    @Test
    void presentHighConfidenceTitleIsShownPlain() {
        String reply = composer().compose(new DraftSummary(
            7L, "tok", "Jazz Night", new BigDecimal("0.95"), null, null, List.of()));
        assertThat(reply).contains("Title: Jazz Night");
        assertThat(reply).doesNotContain("⚠️");
    }

    @Test
    void lowConfidenceTitleIsFlagged() {
        String reply = composer().compose(new DraftSummary(
            7L, "tok", "Jzz Nght", new BigDecimal("0.3"), null, null, List.of()));
        assertThat(reply).contains("Jzz Nght");
        assertThat(reply).contains("⚠️");                          // low-confidence flag
    }

    @Test
    void urlsAreListed() {
        String reply = composer().compose(new DraftSummary(
            9L, "tok", "Show", new BigDecimal("0.9"),
            null, null, List.of("https://a.com", "https://b.com/x")));
        assertThat(reply).contains("https://a.com");
        assertThat(reply).contains("https://b.com/x");
    }

    @Test
    void startAndLocationShownOnlyWhenPresent() {
        String without = composer().compose(new DraftSummary(
            1L, "t", "T", new BigDecimal("0.9"), null, null, List.of()));
        assertThat(without).doesNotContain("Starts:");
        assertThat(without).doesNotContain("Location:");

        String with = composer().compose(new DraftSummary(
            1L, "t", "T", new BigDecimal("0.9"), "Fri 7pm", "Downtown Hall", List.of()));
        assertThat(with).contains("Starts: Fri 7pm");
        assertThat(with).contains("Location: Downtown Hall");
    }

    @Test
    void reviewLinkHandlesTrailingSlashInBaseUrl() {
        WebhookProperties props = new WebhookProperties();
        props.setPublicBaseUrl("https://tunnel.example/");  // trailing slash
        ReplyComposer c = new ReplyComposer(props);
        String reply = c.compose(new DraftSummary(5L, "tk", "T", new BigDecimal("0.9"), null, null, List.of()));
        assertThat(reply).contains("https://tunnel.example/review/5?token=tk"); // no double slash
    }
}
