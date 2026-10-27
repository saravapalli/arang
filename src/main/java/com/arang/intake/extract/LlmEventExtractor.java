package com.arang.intake.extract;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.arang.intake.config.LlmProperties;

import tools.jackson.databind.ObjectMapper;

/**
 * Deciphers event fields via a local OpenAI-compatible chat model (Ollama {@code /v1/chat/
 * completions}). Best-effort: any failure returns {@link ExtractedFields#empty()}. Subject to the
 * shared {@link DailyRequestLimiter} so it respects the global per-day cap.
 */
public class LlmEventExtractor implements EventExtractor {

    private static final Logger log = LoggerFactory.getLogger(LlmEventExtractor.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final LlmProperties props;
    private final DailyRequestLimiter limiter;
    private final RestClient restClient;

    public LlmEventExtractor(LlmProperties props, DailyRequestLimiter limiter) {
        this.props = props;
        this.limiter = limiter;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(10));
        f.setReadTimeout(Duration.ofSeconds(props.getTimeoutSeconds()));
        this.restClient = RestClient.builder().requestFactory(f)
            .baseUrl(props.getOllama().getBaseUrl()).build();
    }

    @Override
    public ExtractedFields extract(String message) {
        if (message == null || message.isBlank()) {
            return ExtractedFields.empty();
        }
        if (!limiter.tryAcquire()) {
            log.warn("Daily LLM request cap ({}) reached — skipping extraction", limiter.getDailyLimit());
            return ExtractedFields.empty();
        }
        try {
            Map<String, Object> payload = Map.of(
                "model", props.getOllama().getModel(),
                "temperature", 0,
                "stream", false,
                "messages", List.of(
                    Map.of("role", "system", "content",
                        ExtractionJsonParser.systemPrompt(java.time.LocalDate.now())),
                    Map.of("role", "user", "content", message)));

            String body = restClient.post()
                .uri("/chat/completions")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .body(payload)
                .exchange((req, res) -> new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8));

            String content = JSON.readTree(body)
                .path("choices").path(0).path("message").path("content").asString();
            return ExtractionJsonParser.parse(content);
        } catch (Exception e) {
            log.warn("Ollama extraction failed ({}): {}", props.getOllama().getModel(), e.toString());
            return ExtractedFields.empty();
        }
    }
}
