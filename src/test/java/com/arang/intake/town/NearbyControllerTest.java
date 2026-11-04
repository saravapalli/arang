package com.arang.intake.town;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * TC-P2.1-050, TC-P2.1-051 (ARANG-TTP-P2-1, task T13): nearby live towns with events, nearest first, excluding the
 * town itself; unknown/hidden slug → 404; a bad window → 400.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NearbyControllerTest {

    @LocalServerPort
    int port;
    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM event WHERE title LIKE 'nearby-test%'");
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private void publishedEventIn(String postal) {
        // starts_at within this week so the default window includes it.
        Instant soon = Instant.now().plusSeconds(2 * 86400);
        jdbc.update("INSERT INTO event (title, starts_at, postal_code, status, source_type, origin_type, "
                + "country_code, latitude, longitude) VALUES ('nearby-test', ?, ?, 'published', 'text', 'user', "
                + "'US', 42.3, -71.4)",
            Timestamp.from(soon), postal);
    }

    @Test
    void listsNearbyLiveTownsWithEvents() throws Exception {
        // Framingham (01701) is ~4 mi from Ashland and live; give it an event this week.
        publishedEventIn("01701");
        HttpResponse<String> r = get("/api/towns/ashland-ma/nearby");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("framingham-ma");
        assertThat(r.body()).doesNotContain("ashland-ma");   // never the town itself
        assertThat(r.body()).contains("distanceMiles");
        assertThat(r.body()).contains("eventCount");
    }

    @Test
    void unknownTownIs404() throws Exception {
        assertThat(get("/api/towns/zzz-xx/nearby").statusCode()).isEqualTo(404);
    }

    @Test
    void badWindowIs400() throws Exception {
        assertThat(get("/api/towns/ashland-ma/nearby?from=2026-12-01&to=2026-11-01").statusCode()).isEqualTo(400);
        assertThat(get("/api/towns/ashland-ma/nearby?from=2026-01-01&to=2026-12-31").statusCode()).isEqualTo(400);
    }
}
