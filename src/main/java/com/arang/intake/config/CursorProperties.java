package com.arang.intake.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config for the Cursor Background Agents API (outbound), bound from {@code arang.cursor.*}.
 * Secrets come from {@code config/cursor-agent.properties} (git-ignored) or env — never hardcoded.
 * See {@code config/cursor-agent.properties.example}.
 */
@ConfigurationProperties(prefix = "arang.cursor")
public class CursorProperties {

    private String apiKey = "";
    private String baseUrl = "https://api.cursor.com";
    private String modelId = "composer-2";
    private boolean fast = true;
    private String repoUrl = "";
    private String startingRef = "main";
    private boolean autoCreatePr = true;
    private String linearApiKey = "";
    private String githubToken = "";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !"YOUR_API_KEY".equals(apiKey);
    }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public boolean isFast() { return fast; }
    public void setFast(boolean fast) { this.fast = fast; }
    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public String getStartingRef() { return startingRef; }
    public void setStartingRef(String startingRef) { this.startingRef = startingRef; }
    public boolean isAutoCreatePr() { return autoCreatePr; }
    public void setAutoCreatePr(boolean autoCreatePr) { this.autoCreatePr = autoCreatePr; }
    public String getLinearApiKey() { return linearApiKey; }
    public void setLinearApiKey(String linearApiKey) { this.linearApiKey = linearApiKey; }
    public String getGithubToken() { return githubToken; }
    public void setGithubToken(String githubToken) { this.githubToken = githubToken; }
}
