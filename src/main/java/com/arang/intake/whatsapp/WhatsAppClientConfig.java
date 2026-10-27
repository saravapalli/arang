package com.arang.intake.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.arang.intake.config.WebhookProperties;

/**
 * Selects the {@link WhatsAppClient} implementation deterministically from config: the real
 * {@link CloudApiWhatsAppClient} when an access token is configured, otherwise the no-op
 * {@link CapturingClient} (offline dev + tests). A single factory avoids the ambiguity of
 * {@code @ConditionalOnProperty} with empty/absent string values.
 */
@Configuration
public class WhatsAppClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppClientConfig.class);

    @Bean
    public WhatsAppClient whatsAppClient(WebhookProperties props) {
        String token = props.getAccessToken();
        if (token != null && !token.isBlank()) {
            log.info("WhatsApp Cloud API client active (access token configured)");
            return new CloudApiWhatsAppClient(props);
        }
        log.info("No WhatsApp access token configured — using no-op logging client");
        return new CapturingClient();
    }

    /**
     * No-op client: logs the outbound message and captures the last one so wiring tests can assert
     * a reply was sent without any network.
     */
    public static class CapturingClient implements WhatsAppClient {

        private static final Logger log = LoggerFactory.getLogger(CapturingClient.class);

        private volatile String lastToPhone;
        private volatile String lastBody;
        private volatile int sendCount;

        @Override
        public void sendText(String toPhone, String body) {
            this.lastToPhone = toPhone;
            this.lastBody = body;
            this.sendCount++;
            log.info("[no-op WhatsApp] would send to {}:\n{}", toPhone, body);
        }

        public String getLastToPhone() {
            return lastToPhone;
        }

        public String getLastBody() {
            return lastBody;
        }

        public int getSendCount() {
            return sendCount;
        }
    }
}
