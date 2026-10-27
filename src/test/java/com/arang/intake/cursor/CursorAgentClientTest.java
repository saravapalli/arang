package com.arang.intake.cursor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.arang.intake.config.CursorProperties;

/** Verifies the Cursor API request payload shape + that secrets come from config. No network. */
class CursorAgentClientTest {

    private CursorProperties props() {
        CursorProperties p = new CursorProperties();
        p.setApiKey("test-key");
        p.setModelId("composer-2");
        p.setFast(true);
        p.setRepoUrl("https://github.com/your-org/your-repo");
        p.setStartingRef("main");
        p.setAutoCreatePr(true);
        return p;
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsCorePayloadShape() {
        var client = new CursorAgentClient(props());
        Map<String, Object> body = client.buildPayload("Add a README with setup instructions");

        assertThat(((Map<String, Object>) body.get("prompt")).get("text"))
            .isEqualTo("Add a README with setup instructions");
        Map<String, Object> model = (Map<String, Object>) body.get("model");
        assertThat(model.get("id")).isEqualTo("composer-2");
        List<Map<String, Object>> repos = (List<Map<String, Object>>) body.get("repos");
        assertThat(repos.get(0).get("url")).isEqualTo("https://github.com/your-org/your-repo");
        assertThat(repos.get(0).get("startingRef")).isEqualTo("main");
        assertThat(body.get("autoCreatePR")).isEqualTo(true);
        // no MCP servers when no secrets configured
        assertThat(body.containsKey("mcpServers")).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void includesMcpServersWhenSecretsPresent() {
        CursorProperties p = props();
        p.setLinearApiKey("lin_123");
        p.setGithubToken("ghp_123");
        var client = new CursorAgentClient(p);

        List<Map<String, Object>> mcp = (List<Map<String, Object>>) client.buildPayload("x").get("mcpServers");
        assertThat(mcp).hasSize(2);
        assertThat(mcp).anyMatch(s -> "linear".equals(s.get("name")));
        assertThat(mcp).anyMatch(s -> "github".equals(s.get("name")));
    }

    @Test
    void placeholderKeysAreTreatedAsNotConfigured() {
        CursorProperties p = new CursorProperties(); // default api-key is blank
        assertThat(p.isConfigured()).isFalse();
        p.setApiKey("YOUR_API_KEY");
        assertThat(p.isConfigured()).isFalse(); // template placeholder not treated as real
    }
}
