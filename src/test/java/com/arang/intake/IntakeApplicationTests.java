package com.arang.intake;

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
 * Skeleton smoke test (SDD T1): proves the app context boots, Flyway V1 applies against H2,
 * and the health endpoint responds. Uses the JDK's built-in HttpClient so it stays independent
 * of Spring Boot test-client package relocations across major versions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IntakeApplicationTests {

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
        // If the context fails (bad config, failed migration), this test fails to start.
    }

    @Test
    void healthEndpointIsUp() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/health"))
            .GET()
            .build();

        HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).contains("\"status\":\"UP\"");
    }
}
