package com.arang.intake.whatsapp;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import com.arang.intake.config.WebhookProperties;

/**
 * Real WhatsApp Cloud API sender (REQ-08): {@code POST /{phone-number-id}/messages} with a Bearer
 * token. Instantiated by {@code WhatsAppClientConfig} only when an access token is configured;
 * otherwise the no-op logging client is used, so the app runs offline until credentials exist.
 */
public class CloudApiWhatsAppClient implements WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(CloudApiWhatsAppClient.class);
    private static final String GRAPH_BASE = "https://graph.facebook.com/v21.0";

    private final WebhookProperties props;
    private final RestClient restClient;

    public CloudApiWhatsAppClient(WebhookProperties props) {
        this.props = props;
        this.restClient = RestClient.create(GRAPH_BASE);
    }

    @Override
    public void sendText(String toPhone, String body) {
        Map<String, Object> payload = Map.of(
            "messaging_product", "whatsapp",
            "to", toPhone,
            "type", "text",
            "text", Map.of("preview_url", true, "body", body)
        );
        restClient.post()
            .uri("/{phoneNumberId}/messages", props.getPhoneNumberId())
            .header("Authorization", "Bearer " + props.getAccessToken())
            .body(payload)
            .retrieve()
            .toBodilessEntity();
        log.debug("Sent WhatsApp reply to {}", toPhone);
    }
}
