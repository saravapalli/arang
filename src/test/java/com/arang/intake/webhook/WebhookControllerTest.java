package com.arang.intake.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.arang.intake.dao.ProcessedMessageDao;

/**
 * T2 webhook tests — REQ-01 (verify + receive) and REQ-07 (idempotency).
 * Uses the real WebhookService + ledger repository against H2 (test profile).
 */
@SpringBootTest
@ActiveProfiles("test")
class WebhookControllerTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProcessedMessageDao ledger;

    private MockMvc mvc;

    private static final String VALID_TOKEN = "test-verify-token"; // from application-test.yml

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        ledger.deleteAll();
    }

    // ---- REQ-01: GET verification handshake ----

    @Test
    void getWithValidTokenEchoesChallenge() throws Exception {
        mvc.perform(get("/webhook")
                .param("hub.mode", "subscribe")
                .param("hub.verify_token", VALID_TOKEN)
                .param("hub.challenge", "challenge-123"))
            .andExpect(status().isOk())
            .andExpect(content().string("challenge-123"));
    }

    @Test
    void getWithInvalidTokenIsRejected() throws Exception {
        mvc.perform(get("/webhook")
                .param("hub.mode", "subscribe")
                .param("hub.verify_token", "wrong-token")
                .param("hub.challenge", "challenge-123"))
            .andExpect(status().isForbidden());
    }

    // ---- REQ-01 + REQ-07: POST receive + idempotency ----

    @Test
    void postNewMessageIsRecordedInLedger() throws Exception {
        mvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(messagePayload("wamid.AAA")))
            .andExpect(status().isOk());

        assertThat(ledger.existsById("wamid.AAA")).isTrue();
        assertThat(ledger.count()).isEqualTo(1);
    }

    @Test
    void redeliveryOfSameMessageCreatesNoDuplicate() throws Exception {
        String payload = messagePayload("wamid.BBB");
        mvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk());
        // redelivery
        mvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk());

        assertThat(ledger.count()).isEqualTo(1); // REQ-07: no duplicate
    }

    @Test
    void statusOnlyCallbackIsIgnored() throws Exception {
        // delivery/read receipt: a "statuses" array, no "messages"
        String statusPayload = """
            {"entry":[{"changes":[{"value":{"statuses":[{"id":"wamid.STATUS","status":"delivered"}]}}]}]}
            """;
        mvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusPayload))
            .andExpect(status().isOk());

        assertThat(ledger.count()).isZero(); // nothing recorded
    }

    private static String messagePayload(String messageId) {
        return """
            {"entry":[{"changes":[{"value":{"messages":[{"id":"%s","type":"text","text":{"body":"hello"}}]}}]}]}
            """.formatted(messageId);
    }
}
