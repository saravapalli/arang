package com.arang.intake.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM extraction config, bound from {@code arang.llm.*}. Disabled by default so the app and tests
 * run offline/fast. When enabled, {@code provider} selects the backend:
 * <ul>
 *   <li>{@code ollama} — local OpenAI-compatible endpoint (default).</li>
 *   <li>{@code gemini} — Google Gemini REST API, rate-limited by {@code dailyLimit} to protect a
 *       trial key.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "arang.llm")
public class LlmProperties {

    /** Master switch. When false, the rule-based extractor is used regardless of provider. */
    private boolean enabled = false;

    /** Which backend when enabled: {@code ollama} | {@code gemini}. */
    private String provider = "ollama";

    /** Per-call timeout in seconds (shared across providers). */
    private int timeoutSeconds = 120;

    /** Hard cap on LLM requests per local day (protects trial quotas). */
    private int dailyLimit = 70;

    private final Ollama ollama = new Ollama();
    private final Gemini gemini = new Gemini();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public int getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
    public Ollama getOllama() { return ollama; }
    public Gemini getGemini() { return gemini; }

    /** Local OpenAI-compatible endpoint config. */
    public static class Ollama {
        private String baseUrl = "http://localhost:11434/v1";
        private String model = "qwen2.5-coder:7b";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    /** Google Gemini REST config. The API key is a secret — set via env/properties, not committed. */
    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-3.5-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
}
