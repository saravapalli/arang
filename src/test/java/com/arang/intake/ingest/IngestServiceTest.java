package com.arang.intake.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.model.Event;
import com.arang.intake.model.EventMetadata;
import com.arang.intake.dao.EventMetadataDao;
import com.arang.intake.dao.EventDao;
import com.arang.intake.dao.ProcessedMessageDao;
import com.arang.intake.dao.UserDao;
import com.arang.intake.service.IngestService;
import com.arang.intake.service.WebhookService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** T3 tests — text intake → draft + URL extraction + confidence + provenance (REQ-02/05/06/15). */
@SpringBootTest
@ActiveProfiles("test")
class IngestServiceTest {

    @Autowired private IngestService ingestService;
    @Autowired private WebhookService webhookService;
    @Autowired private EventDao events;
    @Autowired private EventMetadataDao metadata;
    @Autowired private UserDao users;
    @Autowired private ProcessedMessageDao ledger;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void clean() {
        ledger.deleteAll();
        metadata.deleteAll();
        events.deleteAll();
        users.deleteAll();
    }

    // ---- REQ-02 / REQ-15 / Q5: draft shape ----
    @Test
    void textCreatesDraftWithRawMessageTokenAndProvenance() {
        Long id = ingestService.ingestText("Live jazz this Friday", "15551234567", "Mary").summary().eventId();

        Event e = events.findById(id).orElseThrow();
        assertThat(e.getStatus()).isEqualTo("draft");            // REQ-02
        assertThat(e.getRawMessage()).isEqualTo("Live jazz this Friday");
        assertThat(e.getSourceType()).isEqualTo("text");         // Q5a
        assertThat(e.getOriginType()).isEqualTo("user");         // REQ-15
        assertThat(e.getSubmittedBy()).isNotNull();              // REQ-15
        assertThat(e.getEditToken()).isNotNull();                // Q5b
        assertThat(e.getEditToken()).hasSize(43);
        assertThat(e.getEditTokenExpiresAt()).isNotNull();
    }

    // ---- REQ-05: URL extracted verbatim to metadata ----
    @Test
    void urlIsExtractedVerbatimToMetadata() {
        Long id = ingestService.ingestText("Tickets at https://ex.com/buy?id=7 now", "15551234567", "Mary").summary().eventId();

        List<EventMetadata> md = metadata.findByEventId(id);
        assertThat(md).hasSize(1);
        assertThat(md.get(0).getKind()).isEqualTo("url");
        assertThat(md.get(0).getValue()).isEqualTo("https://ex.com/buy?id=7"); // verbatim, trailing text excluded
        assertThat(md.get(0).getSource()).isEqualTo("text");
    }

    // ---- REQ-06: extracted_confidence = min of extracted; null when none ----
    @Test
    void confidenceIsOneWithUrlAndNullWithout() {
        Long withUrl = ingestService.ingestText("see https://ex.com", "15551234567", "Mary").summary().eventId();
        Long noUrl = ingestService.ingestText("just a plain note", "15551234567", "Mary").summary().eventId();

        assertThat(events.findById(withUrl).orElseThrow().getExtractedConfidence())
            .isEqualByComparingTo("1.00");
        assertThat(events.findById(noUrl).orElseThrow().getExtractedConfidence())
            .isNull();
    }

    // ---- REQ-15: user upsert is idempotent on phone ----
    @Test
    void userIsReusedAcrossMessagesFromSamePhone() {
        ingestService.ingestText("first", "15559999999", "Sam");
        ingestService.ingestText("second", "15559999999", "Sam");
        assertThat(users.count()).isEqualTo(1); // same phone → one user
    }

    @Test
    void missingPhoneStillCreatesEventWithNullSubmitter() {
        Long id = ingestService.ingestText("no sender", null, null).summary().eventId();
        assertThat(events.findById(id).orElseThrow().getSubmittedBy()).isNull();
    }

    // ---- Webhook wiring: text ingested + ledger linked; image stubbed ----
    @Test
    void webhookTextMessageCreatesEventAndLinksLedger() throws Exception {
        JsonNode payload = mapper.readTree("""
            {"entry":[{"changes":[{"value":{
              "contacts":[{"profile":{"name":"Mary"},"wa_id":"15551230000"}],
              "messages":[{"id":"wamid.T3A","from":"15551230000","type":"text","text":{"body":"gig https://ex.com"}}]
            }}]}]}
            """);
        webhookService.receive(payload);

        assertThat(events.count()).isEqualTo(1);
        assertThat(ledger.findEventId("wamid.T3A")).isNotNull(); // linked
    }

    @Test
    void webhookImageMessageCreatesDraftViaFailurePathWhenNoCreds() throws Exception {
        // T7: in the test profile there is no WhatsApp access token, so media fetch fails and the
        // REQ-04 path creates a draft from the caption and marks media not-retrieved.
        JsonNode payload = mapper.readTree("""
            {"entry":[{"changes":[{"value":{
              "messages":[{"id":"wamid.IMG","from":"15551230000","type":"image",
                "image":{"id":"media-1","caption":"flyer https://ex.com/e"}}]
            }}]}]}
            """);
        webhookService.receive(payload);

        assertThat(ledger.existsById("wamid.IMG")).isTrue();  // recorded (REQ-07)
        assertThat(events.count()).isEqualTo(1);              // draft created (REQ-04)
        var e = events.findAll().get(0);
        assertThat(e.getSourceType()).isEqualTo("image");     // REQ-03/04 source_type
    }
}
