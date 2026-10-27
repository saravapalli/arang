package com.arang.intake.extract;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.arang.intake.config.LlmProperties;

import tools.jackson.databind.ObjectMapper;

/**
 * Deciphers event fields via the Google Gemini REST API
 * ({@code POST /v1beta/models/{model}:generateContent?key=API_KEY}).
 *
 * <p>Supports both text and <b>multimodal</b> input: a flyer image is sent as an {@code inline_data}
 * part so Gemini reads the poster directly (no separate OCR step). Every call is gated by the
 * shared {@link DailyRequestLimiter} (default cap 70/day) to protect a trial key. Best-effort:
 * transient 503/429 are retried with backoff; any failure returns {@link ExtractedFields#empty()}
 * rather than throwing.
 */
public class GeminiEventExtractor implements EventExtractor {

    private static final Logger log = LoggerFactory.getLogger(GeminiEventExtractor.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_ATTEMPTS = 3;

    private final LlmProperties props;
    private final DailyRequestLimiter limiter;
    private final RestClient restClient;

    public GeminiEventExtractor(LlmProperties props, DailyRequestLimiter limiter) {
        this.props = props;
        this.limiter = limiter;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(10));
        f.setReadTimeout(Duration.ofSeconds(props.getTimeoutSeconds()));
        this.restClient = RestClient.builder().requestFactory(f)
            .baseUrl(props.getGemini().getBaseUrl()).build();
    }

    @Override
    public boolean supportsImages() {
        return true;
    }

    @Override
    public ExtractedFields extract(String message) {
        if (message == null || message.isBlank()) {
            return ExtractedFields.empty();
        }
        // Single user content part: the text.
        return callGemini(List.of(Map.of("parts", List.of(Map.of("text", message)))));
    }

    @Override
    public ExtractedFields extractFromImage(byte[] imageBytes, String mimeType, String caption) {
        if (imageBytes == null || imageBytes.length == 0) {
            return ExtractedFields.empty();
        }
        String mime = (mimeType == null || mimeType.isBlank()) ? "image/png" : mimeType;
        String b64 = Base64.getEncoder().encodeToString(imageBytes);

        // User content part: an inline image + (optional) caption text, so Gemini reads the flyer.
        var parts = new java.util.ArrayList<Map<String, Object>>();
        parts.add(Map.of("inline_data", Map.of("mime_type", mime, "data", b64)));
        String instruction = (caption == null || caption.isBlank())
            ? "Extract the event from this flyer image."
            : "Extract the event from this flyer image. Caption: " + caption;
        parts.add(Map.of("text", instruction));

        return callGemini(List.of(Map.of("parts", parts)));
    }

    /** Shared call path: limiter + retry + parse. {@code contents} is the Gemini contents array. */
    private ExtractedFields callGemini(List<Map<String, Object>> contents) {
        if (props.getGemini().getApiKey() == null || props.getGemini().getApiKey().isBlank()) {
            log.warn("Gemini API key not configured — skipping extraction");
            return ExtractedFields.empty();
        }
        if (!limiter.tryAcquire()) {
            log.warn("Daily Gemini request cap ({}) reached — skipping extraction (resets at local midnight)",
                limiter.getDailyLimit());
            return ExtractedFields.empty();
        }

        Map<String, Object> payload = Map.of(
            "systemInstruction", Map.of("parts", List.of(Map.of("text",
                ExtractionJsonParser.systemPrompt(java.time.LocalDate.now())))),
            "contents", contents,
            "generationConfig", Map.of("temperature", 0, "responseMimeType", "application/json"));
        String uri = "/models/" + props.getGemini().getModel() + ":generateContent?key="
            + props.getGemini().getApiKey();

        long backoffMs = 1000;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String body = restClient.post()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .body(payload)
                    .exchange((req, res) -> {
                        if (res.getStatusCode().isError()) {
                            throw HttpServerErrorException.create(
                                res.getStatusCode(), res.getStatusText(),
                                res.getHeaders(), res.getBody().readAllBytes(), null);
                        }
                        return new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    });
                String content = JSON.readTree(body)
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text").asString();
                return ExtractionJsonParser.parse(content);
            } catch (HttpServerErrorException | HttpClientErrorException.TooManyRequests e) {
                boolean transientErr = (e instanceof HttpClientErrorException.TooManyRequests)
                    || ((HttpServerErrorException) e).getStatusCode().value() == 503
                    || ((HttpServerErrorException) e).getStatusCode().is5xxServerError();
                if (!transientErr || attempt == MAX_ATTEMPTS) {
                    log.warn("Gemini extraction failed after {} attempt(s) ({}): {}",
                        attempt, props.getGemini().getModel(), e.getMessage());
                    return ExtractedFields.empty();
                }
                log.info("Gemini transient error (attempt {}/{}), retrying in {}ms…",
                    attempt, MAX_ATTEMPTS, backoffMs);
                sleep(backoffMs);
                backoffMs *= 2;
            } catch (Exception e) {
                log.warn("Gemini extraction failed ({}): {}", props.getGemini().getModel(), e.toString());
                return ExtractedFields.empty();
            }
        }
        return ExtractedFields.empty();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
