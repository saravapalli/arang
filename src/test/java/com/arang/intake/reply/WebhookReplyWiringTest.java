package com.arang.intake.reply;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.dao.EventMetadataDao;
import com.arang.intake.dao.EventDao;
import com.arang.intake.dao.ProcessedMessageDao;
import com.arang.intake.dao.UserDao;
import com.arang.intake.service.WebhookService;
import com.arang.intake.whatsapp.WhatsAppClientConfig.CapturingClient;
import com.arang.intake.whatsapp.WhatsAppClient;

import tools.jackson.databind.ObjectMapper;

/**
 * T4 wiring: a text webhook message results in exactly one reply, sent to the sender, containing
 * the review link (REQ-08, AC-005). No token is configured in the test profile, so the no-op
 * {@link CapturingClient} is active and we assert on what it captured — no network.
 */
@SpringBootTest
@ActiveProfiles("test")
class WebhookReplyWiringTest {

    @Autowired private WebhookService webhookService;
    @Autowired private WhatsAppClient whatsAppClient;
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

    @Test
    void textMessageSendsExactlyOneReplyToSenderWithReviewLink() throws Exception {
        // the no-op client is the one wired when no access token is set
        assertThat(whatsAppClient).isInstanceOf(CapturingClient.class);
        CapturingClient capturing = (CapturingClient) whatsAppClient;
        int before = capturing.getSendCount();

        webhookService.receive(mapper.readTree("""
            {"entry":[{"changes":[{"value":{
              "contacts":[{"profile":{"name":"Mary"},"wa_id":"15551230000"}],
              "messages":[{"id":"wamid.R1","from":"15551230000","type":"text","text":{"body":"gig https://ex.com/t"}}]
            }}]}]}
            """));

        assertThat(capturing.getSendCount()).isEqualTo(before + 1);   // exactly one reply (AC-005)
        assertThat(capturing.getLastToPhone()).isEqualTo("15551230000"); // to the sender
        assertThat(capturing.getLastBody()).contains("/review/");     // review link present
        assertThat(capturing.getLastBody()).contains("https://ex.com/t"); // echoed URL
    }

    @Test
    void duplicateMessageSendsNoSecondReply() throws Exception {
        String payload = """
            {"entry":[{"changes":[{"value":{
              "contacts":[{"profile":{"name":"Mary"},"wa_id":"15551230000"}],
              "messages":[{"id":"wamid.R2","from":"15551230000","type":"text","text":{"body":"hello"}}]
            }}]}]}
            """;
        CapturingClient capturing = (CapturingClient) whatsAppClient;
        int before = capturing.getSendCount();

        webhookService.receive(mapper.readTree(payload));
        webhookService.receive(mapper.readTree(payload)); // redelivery

        assertThat(capturing.getSendCount()).isEqualTo(before + 1); // only the first triggers a reply
    }
}
