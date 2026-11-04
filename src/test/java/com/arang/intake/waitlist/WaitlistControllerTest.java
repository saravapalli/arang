package com.arang.intake.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

/**
 * TC-P2.1-035..041, TC-P2.1-043 (ARANG-TTP-P2-1, tasks T10/T11): the HTTP endpoint's status codes, the throttle,
 * the identical body for new vs repeat, and that an email never reaches any log at INFO or above (REQ-15).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WaitlistControllerTest {

    @TempDir
    static Path tempDir;
    static Path geoFile;

    @BeforeAll
    static void buildPlaceFile() throws Exception {
        geoFile = tempDir.resolve("geo.sqlite");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:file:" + geoFile.toAbsolutePath());
             Statement s = c.createStatement()) {
            s.execute("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)");
            s.execute("INSERT INTO meta VALUES ('schema_version','1'),('data_date','2026-11-04'),('credits','[]')");
            s.execute("CREATE TABLE place (source_id TEXT, name TEXT, search_name TEXT, type TEXT, state TEXT, "
                + "slug TEXT, latitude REAL, longitude REAL)");
            s.execute("INSERT INTO place VALUES ('1','Worcester','worcester','city','MA','worcester-ma',42.2,-71.8)");
        }
        geoFile.toFile().setReadOnly();
    }

    @AfterAll
    static void cleanup() {
        geoFile.toFile().setWritable(true);
        geoFile.toFile().delete();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("arang.geo.file", () -> geoFile.toString());
        // High limits so the status-code tests are not throttled; the 429 case uses its own resolver below.
        r.add("arang.waitlist.rate-per-minute", () -> "1000");
        r.add("arang.waitlist.global-rate-per-minute", () -> "10000");
    }

    @LocalServerPort
    int port;
    @Autowired
    JdbcTemplate jdbc;

    private HttpResponse<String> post(String json) throws Exception {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/api/waitlist"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void validPostSavesAndRepeatReturnsIdenticalBody() throws Exception {
        HttpResponse<String> first = post("{\"place\":\"worcester-ma\",\"email\":\"saver@example.com\"}");
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.body()).isEqualTo("{\"status\":\"saved\"}");
        HttpResponse<String> repeat = post("{\"place\":\"worcester-ma\",\"email\":\"saver@example.com\"}");
        assertThat(repeat.body()).isEqualTo(first.body());
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM demand_signal s JOIN `user` u ON u.id = s.user_id "
                + "WHERE u.email = 'saver@example.com'", Integer.class)).isEqualTo(1);
    }

    @Test
    void invalidEmailReturns400WithoutEchoingInput() throws Exception {
        HttpResponse<String> r = post("{\"place\":\"worcester-ma\",\"email\":\"not-an-email\"}");
        assertThat(r.statusCode()).isEqualTo(400);
        assertThat(r.body()).contains("\"email\":\"invalid\"");
        assertThat(r.body()).doesNotContain("not-an-email");
    }

    @Test
    void liveTownSlugIsRejected() throws Exception {
        assertThat(post("{\"place\":\"ashland-ma\",\"email\":\"x@example.com\"}").statusCode()).isEqualTo(400);
    }

    @Test
    void manyPostsSucceedUnderAHighLimit() throws Exception {
        // With a high limit the endpoint keeps answering 200; the 5/min cap and its 429 are covered by
        // RequestThrottleTest. This only confirms the throttle is wired in and does not block normal use.
        for (int i = 0; i < 8; i++) {
            assertThat(post("{\"place\":\"worcester-ma\",\"email\":\"ok" + i + "@example.com\"}").statusCode())
                .isEqualTo(200);
        }
    }

    @Test
    void emailNeverAppearsInLogsAtInfoOrAbove() throws Exception {
        ch.qos.logback.classic.Logger root =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        appender.start();
        root.addAppender(appender);
        try {
            String secret = "logcheck-secret@example.com";
            post("{\"place\":\"worcester-ma\",\"email\":\"" + secret + "\"}");
            post("{\"place\":\"worcester-ma\",\"email\":\"" + secret + "\"}");    // duplicate path
            post("{\"place\":\"worcester-ma\",\"email\":\"" + secret + "\"}");
            // malformed JSON containing the email
            post("{\"place\":\"worcester-ma\",\"email\":\"" + secret + "\" ");
            List<String> messages = new ArrayList<>();
            for (ILoggingEvent e : appender.list) {
                if (e.getLevel().isGreaterOrEqual(Level.INFO)) {
                    messages.add(e.getFormattedMessage());
                }
            }
            assertThat(messages).noneMatch(m -> m.contains(secret));
        } finally {
            root.detachAppender(appender);
        }
    }
}
