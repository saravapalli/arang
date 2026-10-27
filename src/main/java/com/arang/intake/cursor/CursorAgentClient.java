package com.arang.intake.cursor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.arang.intake.config.CursorProperties;

/**
 * Outbound client for the Cursor Background Agents API — {@code POST /v1/agents}.
 * Auth is HTTP Basic with the API key as the username and an empty password (the {@code -u KEY:}
 * form). All secrets (Cursor key, Linear key, GitHub token) come from {@link CursorProperties}
 * (loaded from the git-ignored {@code config/cursor-agent.properties}); none are hardcoded.
 *
 * <p>Separate, self-contained integration — unrelated to the event-intake pipeline.
 */
@Service
public class CursorAgentClient {

    private static final Logger log = LoggerFactory.getLogger(CursorAgentClient.class);

    private final CursorProperties props;
    private final RestClient http;

    public CursorAgentClient(CursorProperties props) {
        this.props = props;
        this.http = RestClient.create();
    }

    /**
     * Launch an agent with the given prompt (uses the configured default model/repo/MCP servers).
     *
     * @return the raw JSON response body from Cursor.
     * @throws IllegalStateException if no API key is configured.
     */
    public String launchAgent(String promptText) {
        if (!props.isConfigured()) {
            throw new IllegalStateException(
                "Cursor API key not configured. Set arang.cursor.api-key in config/cursor-agent.properties");
        }
        Map<String, Object> payload = buildPayload(promptText);
        byte[] raw = http.post()
            .uri(props.getBaseUrl() + "/v1/agents")
            .header("Authorization", basicAuth(props.getApiKey()))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .body(payload)
            .retrieve()
            .body(byte[].class);
        String body = raw == null ? "" : new String(raw, StandardCharsets.UTF_8);
        log.info("Launched Cursor agent (model={}, repo={})", props.getModelId(), props.getRepoUrl());
        return body;
    }

    /** Builds the request body in the API's shape. Package-visible so it can be unit-tested. */
    Map<String, Object> buildPayload(String promptText) {
        Map<String, Object> model = Map.of(
            "id", props.getModelId(),
            "params", List.of(Map.of("id", "fast", "value", String.valueOf(props.isFast()))));

        Map<String, Object> repo = Map.of(
            "url", props.getRepoUrl(),
            "startingRef", props.getStartingRef());

        var mcpServers = new java.util.ArrayList<Map<String, Object>>();
        if (notBlank(props.getLinearApiKey())) {
            mcpServers.add(Map.of(
                "name", "linear", "type", "http", "url", "https://mcp.linear.app/sse",
                "headers", Map.of("Authorization", "Bearer " + props.getLinearApiKey())));
        }
        if (notBlank(props.getGithubToken())) {
            mcpServers.add(Map.of(
                "name", "github", "type", "stdio", "command", "npx",
                "args", List.of("-y", "@modelcontextprotocol/server-github"),
                "env", Map.of("GITHUB_TOKEN", props.getGithubToken())));
        }

        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("prompt", Map.of("text", promptText));
        body.put("model", model);
        body.put("repos", List.of(repo));
        if (!mcpServers.isEmpty()) {
            body.put("mcpServers", mcpServers);
        }
        body.put("autoCreatePR", props.isAutoCreatePr());
        return body;
    }

    private static String basicAuth(String apiKey) {
        // "-u API_KEY:" → base64("API_KEY:")
        String token = Base64.getEncoder()
            .encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank() && !s.startsWith("YOUR_");
    }
}
