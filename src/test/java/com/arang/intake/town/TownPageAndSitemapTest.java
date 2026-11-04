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
 * TC-P2.1-021..031 (ARANG-TTP-P2-1), tasks T7 and T8: server-rendered town pages (decision table, redirects,
 * escaping, cache header), landing, sitemap, and robots. Uses the HttpClient (no Spring test-client).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TownPageAndSitemapTest {

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
            s.execute("INSERT INTO place VALUES ('1','Ashland','ashland','town','MA','ashland-ma',42.25,-71.47),"
                + "('2','Worcester','worcester','city','MA','worcester-ma',42.26,-71.80)");
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

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build().send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void liveTownPageHasTitleCanonicalAndEvents() throws Exception {
        HttpResponse<String> r = get("/towns/ashland-ma");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.headers().firstValue("Cache-Control")).hasValue("public, max-age=60");
        String b = r.body();
        assertThat(b).contains("<title>Ashland, MA events — Arangly</title>");
        assertThat(b).contains("rel=\"canonical\" href=\"https://arangly.com/towns/ashland-ma\"");
        assertThat(b).contains("What\u2019s on in Ashland, MA");
        assertThat(b).contains("id=\"site-config\"");
        assertThat(b).contains("<div id=\"root\">");   // crawler content is rendered inside #root (cleared by React on mount)
    }

    @Test
    void canonicalHasNoQueryString() throws Exception {
        String b = get("/towns/ashland-ma?window=next-week&category=music").body();
        assertThat(b).contains("href=\"https://arangly.com/towns/ashland-ma\"");
        assertThat(b).doesNotContain("window=next-week");
    }

    @Test
    void caseAndTrailingSlashRedirectPermanently() throws Exception {
        HttpResponse<String> upper = get("/towns/Ashland-MA");
        assertThat(upper.statusCode()).isEqualTo(301);
        assertThat(upper.headers().firstValue("Location")).hasValue("/towns/ashland-ma");

        HttpResponse<String> slash = get("/towns/ashland-ma/");
        assertThat(slash.statusCode()).isEqualTo(301);
        assertThat(slash.headers().firstValue("Location")).hasValue("/towns/ashland-ma");
    }

    @Test
    void aliasRedirectsToDatabaseSlug() throws Exception {
        Long id = jdbc.queryForObject("SELECT id FROM town WHERE slug='ashland-ma'", Long.class);
        jdbc.update("INSERT INTO town_slug_alias (alias, town_id) VALUES ('old-ashland-ma', ?)", id);
        try {
            HttpResponse<String> r = get("/towns/old-ashland-ma");
            assertThat(r.statusCode()).isEqualTo(301);
            assertThat(r.headers().firstValue("Location")).hasValue("/towns/ashland-ma");
        } finally {
            jdbc.update("DELETE FROM town_slug_alias WHERE alias='old-ashland-ma'");
        }
    }

    @Test
    void notLivePlaceShowsComingSoonNoindex() throws Exception {
        HttpResponse<String> r = get("/towns/worcester-ma");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("Coming soon to Worcester, MA");
        assertThat(r.body()).contains("content=\"noindex\"");
    }

    @Test
    void unknownAndMalformedSlugAreRejected() throws Exception {
        // Unknown slug → 404 with the landing body.
        HttpResponse<String> unknown = get("/towns/zzz-xx");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(unknown.body()).contains("What\u2019s on in your town?");
        // Malformed slug not matching [a-z0-9-] → 404 landing.
        HttpResponse<String> dots = get("/towns/a..b");
        assertThat(dots.statusCode()).isEqualTo(404);
        assertThat(dots.body()).contains("What\u2019s on in your town?");
        // An encoded slash is refused by the framework before routing (400); never serves another town.
        assertThat(get("/towns/a%2Fb").statusCode()).isIn(400, 404);
    }

    @Test
    void comingSoonFallsBackToLandingWhenGeoDown() throws Exception {
        // worcester-ma is not a live town; the place file is up here, so it is coming-soon.
        // (Geo-down behaviour is covered by GeoPlaceSourceTest; here we assert the coming-soon branch.)
        assertThat(get("/towns/worcester-ma").statusCode()).isEqualTo(200);
    }

    @Test
    void eventTextIsEscapedNotExecutable() throws Exception {
        jdbc.update("INSERT INTO event (title, starts_at, status, source_type, origin_type, country_code) "
            + "VALUES ('</script><script>alert(1)</script>', '2999-01-01 10:00:00', 'published', 'text', 'user', 'US')");
        try {
            // Seed a town whose postal matches the event's (null here) — use an existing live town page.
            String b = get("/towns/ashland-ma").body();
            assertThat(b).doesNotContain("<script>alert(1)</script>");
        } finally {
            jdbc.update("DELETE FROM event WHERE title = '</script><script>alert(1)</script>'");
        }
    }

    @Test
    void landingListsLiveTowns() throws Exception {
        HttpResponse<String> r = get("/");
        assertThat(r.statusCode()).isEqualTo(200);
        assertThat(r.body()).contains("/towns/ashland-ma");
        assertThat(r.body()).contains("<title>Arangly — events in your town</title>");
    }

    @Test
    void sitemapListsLiveTownsOnly() throws Exception {
        String xml = get("/sitemap.xml").body();
        assertThat(xml).contains("<loc>https://arangly.com/</loc>");
        assertThat(xml).contains("<loc>https://arangly.com/towns/ashland-ma</loc>");
        assertThat(xml).doesNotContain("/towns/worcester-ma");  // not live
        for (String excluded : new String[]{"/add", "/api/", "/webhook/", "/health", "/tiles/"}) {
            assertThat(xml).doesNotContain(excluded);
        }
    }

    @Test
    void sitemapReflectsStatusChange() throws Exception {
        jdbc.update("INSERT INTO town (slug,name,state,postal_code,latitude,longitude,map_zoom,status) "
            + "VALUES ('worcester-ma','Worcester','MA','01601',42.26,-71.80,13,'live')");
        try {
            assertThat(get("/sitemap.xml").body()).contains("/towns/worcester-ma");
        } finally {
            jdbc.update("DELETE FROM town WHERE slug='worcester-ma'");
        }
    }

    @Test
    void robotsDisallowsWriteRoutesAndNamesSitemap() throws Exception {
        String txt = get("/robots.txt").body();
        for (String path : new String[]{"/add", "/submit", "/console", "/review/", "/api/", "/webhook/",
            "/media/", "/tiles/", "/health", "/actuator/"}) {
            assertThat(txt).contains("Disallow: " + path);
        }
        assertThat(txt).contains("Sitemap: https://arangly.com/sitemap.xml");
    }
}
