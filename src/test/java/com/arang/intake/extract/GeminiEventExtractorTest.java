package com.arang.intake.extract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.arang.intake.config.LlmProperties;

/**
 * Gemini extractor guard behavior (no network): skips when no key is configured, and skips when
 * the daily cap is exhausted. JSON parsing itself is covered by {@link ExtractionJsonParserTest}.
 */
class GeminiEventExtractorTest {

    @Test
    void returnsEmptyWhenNoApiKey() {
        LlmProperties props = new LlmProperties(); // gemini.apiKey blank by default
        GeminiEventExtractor ex = new GeminiEventExtractor(props, new DailyRequestLimiter(70));
        assertThat(ex.extract("Jazz Friday 8pm").hasAnyField()).isFalse();
    }

    @Test
    void respectsDailyCapWithoutCallingApi() {
        LlmProperties props = new LlmProperties();
        props.getGemini().setApiKey("test-key");
        // cap 0 → limiter blocks before any HTTP call is attempted
        GeminiEventExtractor ex = new GeminiEventExtractor(props, new DailyRequestLimiter(0));
        assertThat(ex.extract("Jazz Friday 8pm").hasAnyField()).isFalse();
    }

    @Test
    void supportsImages() {
        assertThat(new GeminiEventExtractor(new LlmProperties(), new DailyRequestLimiter(70))
            .supportsImages()).isTrue();
    }

    @Test
    void imageExtractionGuards() {
        LlmProperties props = new LlmProperties();
        props.getGemini().setApiKey("test-key");
        GeminiEventExtractor ex = new GeminiEventExtractor(props, new DailyRequestLimiter(70));
        // empty bytes → empty, no network
        assertThat(ex.extractFromImage(new byte[0], "image/png", null).hasAnyField()).isFalse();
        // cap 0 → blocked before any HTTP call
        GeminiEventExtractor capped = new GeminiEventExtractor(props, new DailyRequestLimiter(0));
        assertThat(capped.extractFromImage(new byte[]{1, 2, 3}, "image/png", "flyer").hasAnyField()).isFalse();
    }
}
