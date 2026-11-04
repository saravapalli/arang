package com.arang.intake.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.arang.intake.waitlist.WaitlistService.Result;

/**
 * TC-P2.1-035..038 (ARANG-TTP-P2-1, task T10): validation, place resolution, idempotency, and the unverified
 * marker, on H2. Concurrency (TC-033) is a separate MySQL-only test.
 */
@SpringBootTest
@ActiveProfiles("test")
class WaitlistServiceTest {

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
    }

    @Autowired
    WaitlistService service;
    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM demand_signal");
        jdbc.update("DELETE FROM `user` WHERE email IS NOT NULL");
    }

    @Test
    void savesValidSignalLowercaseEmailUnverified() {
        assertThat(service.save("worcester-ma", "A@B.Co")).isEqualTo(Result.SAVED);
        assertThat(jdbc.queryForObject("SELECT email FROM `user` WHERE email IS NOT NULL", String.class))
            .isEqualTo("a@b.co");
        var row = jdbc.queryForMap("SELECT place_key, place_label, consent_text, verified_at FROM demand_signal");
        assertThat(row.get("place_key")).isEqualTo("worcester-ma");
        assertThat(row.get("place_label")).isEqualTo("Worcester, MA");
        assertThat(row.get("consent_text").toString()).contains("Worcester, MA");
        assertThat(row.get("verified_at")).isNull();
    }

    @Test
    void rejectsInvalidEmails() {
        assertThat(service.save("worcester-ma", "a".repeat(255) + "@b.co")).isEqualTo(Result.INVALID_EMAIL);
        assertThat(service.save("worcester-ma", "nope")).isEqualTo(Result.INVALID_EMAIL);
        assertThat(service.save("worcester-ma", "a@@b.co")).isEqualTo(Result.INVALID_EMAIL);
        assertThat(service.save("worcester-ma", "@b.co")).isEqualTo(Result.INVALID_EMAIL);
        assertThat(service.save("worcester-ma", "a@")).isEqualTo(Result.INVALID_EMAIL);
        assertThat(service.save("worcester-ma", "a b@c.co")).isEqualTo(Result.INVALID_EMAIL);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM demand_signal", Integer.class)).isZero();
    }

    @Test
    void acceptsBoundaryEmailAndTrims() {
        String local = "a".repeat(254 - "@b.co".length());
        assertThat(service.save("worcester-ma", "  " + local + "@b.co  ")).isEqualTo(Result.SAVED);
    }

    @Test
    void rejectsLiveTownSlug() {
        assertThat(service.save("ashland-ma", "a@b.co")).isEqualTo(Result.INVALID_PLACE);
    }

    @Test
    void rejectsEmptyOrOverlongPlace() {
        assertThat(service.save("", "a@b.co")).isEqualTo(Result.INVALID_PLACE);
        assertThat(service.save("x".repeat(81), "a@b.co")).isEqualTo(Result.INVALID_PLACE);
    }

    @Test
    void freeTextPlaceIsStoredAsTextKey() {
        assertThat(service.save("Nashua", "a@b.co")).isEqualTo(Result.SAVED);
        assertThat(jdbc.queryForObject("SELECT place_key FROM demand_signal", String.class)).isEqualTo("text:nashua");
        assertThat(jdbc.queryForObject("SELECT place_label FROM demand_signal", String.class)).isEqualTo("Nashua");
    }

    @Test
    void repeatIsIdempotentAndSecondPlaceAddsOneSignal() {
        assertThat(service.save("worcester-ma", "a@b.co")).isEqualTo(Result.SAVED);
        assertThat(service.save("worcester-ma", "a@b.co")).isEqualTo(Result.SAVED);
        assertThat(service.save("worcester-ma", "A@B.CO")).isEqualTo(Result.SAVED);  // same user after lowercasing
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user` WHERE email='a@b.co'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM demand_signal", Integer.class)).isEqualTo(1);

        assertThat(service.save("Nashua", "a@b.co")).isEqualTo(Result.SAVED);  // second place
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM `user` WHERE email='a@b.co'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM demand_signal", Integer.class)).isEqualTo(2);
    }
}
