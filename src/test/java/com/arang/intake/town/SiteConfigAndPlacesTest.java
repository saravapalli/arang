package com.arang.intake.town;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * TC-P2.1-012 (site-config), TC-P2.1-014..020 (places search), TC-P2.1-020 (live flag) — ARANG-TTP-P2-1,
 * tasks T5 and T6. A small place file is built once into a temp folder and the test profile points at it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SiteConfigAndPlacesTest {

    @TempDir
    static Path tempDir;
    static Path geoFile;

    @BeforeAll
    static void buildPlaceFile() throws Exception {
        geoFile = tempDir.resolve("geo-test.sqlite");
        String url = "jdbc:sqlite:file:" + geoFile.toAbsolutePath();
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)");
            s.execute("INSERT INTO meta VALUES ('schema_version','1'),('data_date','2026-11-04'),"
                + "('credits','[\"Town data: U.S. Census Bureau\"]')");
            s.execute("CREATE TABLE place (source_id TEXT, name TEXT, search_name TEXT, type TEXT, state TEXT, "
                + "slug TEXT, latitude REAL, longitude REAL)");
            // Ashland is a live seeded town; Worcester is a real place but not live.
            s.execute("INSERT INTO place VALUES "
                + "('1','Ashland','ashland','town','MA','ashland-ma',42.25,-71.47),"
                + "('2','Worcester','worcester','city','MA','worcester-ma',42.26,-71.80),"
                + "('3','Framingham','framingham','town','MA','framingham-ma',42.30,-71.42)");
        }
        geoFile.toFile().setReadOnly();
    }

    @AfterAll
    static void cleanup() throws Exception {
        geoFile.toFile().setWritable(true);
        Files.deleteIfExists(geoFile);
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("arang.geo.file", () -> geoFile.toString());
        r.add("arang.site.public-url", () -> "https://arangly.com");
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private String get(String path) throws Exception {
        HttpResponse<String> resp = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(200);
        return resp.body();
    }

    private int status(String path) throws Exception {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    void siteConfigListsLiveTownsCreditsAndGeoUp() throws Exception {
        String body = get("/api/site-config");
        assertThat(body).contains("\"ashland-ma\"");
        assertThat(body).contains("\"geo\":\"UP\"");
        assertThat(body).contains("Town data: U.S. Census Bureau");
        assertThat(body).contains("GeoNames");                 // always-present credit line
        assertThat(body).contains("\"nearMeMiles\":100");
        assertThat(body).contains("\"publicUrl\":\"https://arangly.com\"");
        assertThat(body).doesNotContain("worcester-ma");       // not a live town
    }

    @Test
    void placesSearchReturnsLiveFlagAndExactMatch() throws Exception {
        String body = get("/api/places?q=ashland");
        assertThat(body).contains("\"slug\":\"ashland-ma\"");
        assertThat(body).contains("\"live\":true");
        assertThat(body).contains("\"exact\":true");

        // Worcester resolves but is not live.
        assertThat(get("/api/places?q=worcester")).contains("\"slug\":\"worcester-ma\"").contains("\"live\":false");
    }

    @Test
    void digitsOnlyReturnsHint() throws Exception {
        assertThat(get("/api/places?q=01721")).contains("\"hint\":\"type_town_name\"");
    }

    @Test
    void invalidQueryIs400() throws Exception {
        assertThat(status("/api/places?q=")).isEqualTo(400);
        assertThat(status("/api/places?q=" + "x".repeat(81))).isEqualTo(400);
    }

    @Test
    void liveFlagFollowsTownStatus() throws Exception {
        // Make Worcester live, then it must show live=true and appear in site-config.
        jdbc.update("INSERT INTO town (slug,name,state,postal_code,latitude,longitude,map_zoom,status) "
            + "VALUES ('worcester-ma','Worcester','MA','01601',42.26,-71.80,13,'live')");
        try {
            assertThat(get("/api/places?q=worcester")).contains("\"live\":true");
            assertThat(get("/api/site-config")).contains("worcester-ma");
        } finally {
            jdbc.update("DELETE FROM town WHERE slug='worcester-ma'");
        }
    }
}
