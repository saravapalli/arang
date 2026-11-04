package com.arang.intake.geo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * TC-P2.1-011 (ARANG-TTP-P2-1): {@code /health} keeps the top-level status UP and adds a {@code geo} section with a
 * fixed reason and no file path, when no place file is configured in the test profile. Proves SDD-P2.1 §4.7.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HealthGeoTest {

    @LocalServerPort
    int port;

    @Test
    void healthReportsGeoUnavailableWithoutLeakingPaths() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> resp = client.send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/health")).GET().build(),
            HttpResponse.BodyHandlers.ofString());

        assertThat(resp.statusCode()).isEqualTo(200);
        String body = resp.body();
        assertThat(body).contains("\"status\":\"UP\"");
        assertThat(body).contains("\"phase\":\"P2.1\"");
        assertThat(body).contains("\"geo\"");
        assertThat(body).contains("\"status\":\"UNAVAILABLE\"");
        assertThat(body).contains("\"reason\":\"NOT_CONFIGURED\"");
        assertThat(body).doesNotContain(".sqlite");
        assertThat(body).doesNotContain("/");  // no file paths in the geo section (URLs would contain a slash)
    }
}
