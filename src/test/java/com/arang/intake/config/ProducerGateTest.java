package com.arang.intake.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The producer gate closes the whole producer surface when a passcode is set: /add and /console show the passcode
 * form, POST /submit and the producer API are refused (401), and the consumer site stays open. Guards the fix for
 * "the public can add events".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProducerGateTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("arang.producer.key", () -> "test-passcode");
    }

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body, String contentType) throws Exception {
        return http.send(HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path))
            .header("Content-Type", contentType).POST(HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void producerPagesShowThePasscodeFormWhenLocked() throws Exception {
        for (String path : new String[]{"/add", "/submit", "/console"}) {
            HttpResponse<String> r = get(path);
            assertThat(r.statusCode()).as(path).isEqualTo(200);
            assertThat(r.body()).as(path).contains("Organizers only");
        }
    }

    @Test
    void postSubmitIsRefusedWhenLocked() throws Exception {
        HttpResponse<String> r = post("/submit", "text=public event", "application/x-www-form-urlencoded");
        assertThat(r.statusCode()).isEqualTo(401);
        assertThat(r.body()).contains("producer_locked");
    }

    @Test
    void producerApiIsRefusedWhenLocked() throws Exception {
        HttpResponse<String> r = post("/api/producer/message", "{}", "application/json");
        assertThat(r.statusCode()).isEqualTo(401);
    }

    @Test
    void correctPasscodeSetsCookieAndRedirects() throws Exception {
        HttpResponse<String> r = get("/add?key=test-passcode");
        assertThat(r.statusCode()).isEqualTo(302);
        assertThat(r.headers().firstValue("Set-Cookie").orElse("")).contains("producer_key=test-passcode");
    }

    @Test
    void consumerSiteStaysOpen() throws Exception {
        assertThat(get("/").statusCode()).isEqualTo(200);
        assertThat(get("/api/places?q=ashland").statusCode()).isIn(200, 503);
        assertThat(get("/sitemap.xml").statusCode()).isEqualTo(200);
    }
}
