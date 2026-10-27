package com.arang.intake.extract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.arang.intake.config.LlmProperties;

/**
 * Selects the {@link EventExtractor} by config. When {@code arang.llm.enabled=false} → the offline
 * {@link RuleBasedEventExtractor}. When enabled, {@code arang.llm.provider} chooses:
 * {@code gemini} → {@link GeminiEventExtractor}, anything else → {@link LlmEventExtractor} (Ollama).
 * The {@link DailyRequestLimiter} is a shared singleton so the per-day cap spans all calls.
 */
@Configuration
public class EventExtractorConfig {

    private static final Logger log = LoggerFactory.getLogger(EventExtractorConfig.class);

    @Bean
    public DailyRequestLimiter dailyRequestLimiter(LlmProperties props) {
        return new DailyRequestLimiter(props.getDailyLimit());
    }

    @Bean
    public EventExtractor eventExtractor(LlmProperties props, DailyRequestLimiter limiter) {
        if (!props.isEnabled()) {
            log.info("LLM event extraction disabled — using rule-based extractor");
            return new RuleBasedEventExtractor();
        }
        if ("gemini".equalsIgnoreCase(props.getProvider())) {
            log.info("LLM event extraction ENABLED → Gemini {} (daily cap {})",
                props.getGemini().getModel(), props.getDailyLimit());
            return new GeminiEventExtractor(props, limiter);
        }
        log.info("LLM event extraction ENABLED → Ollama {} @ {} (daily cap {})",
            props.getOllama().getModel(), props.getOllama().getBaseUrl(), props.getDailyLimit());
        return new LlmEventExtractor(props, limiter);
    }
}
