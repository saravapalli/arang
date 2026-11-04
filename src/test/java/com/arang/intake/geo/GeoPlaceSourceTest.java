package com.arang.intake.geo;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * TC-P2.1-010 and TC-P2.1-011 (ARANG-TTP-P2-1): the place-file reader never fails at startup, reports a fixed
 * reason when the file is missing or wrong, and loads places from a good file. Proves SDD-P2.1 task T4. Plain JUnit
 * (no Spring context) so each case uses its own file.
 */
class GeoPlaceSourceTest {

    /** Write a place file with the given schema version and one place, using the bundled SQLite driver. */
    private Path writeFile(Path dir, String name, int schemaVersion) throws Exception {
        Path path = dir.resolve(name);
        String url = "jdbc:sqlite:file:" + path.toAbsolutePath();
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT)");
            s.execute("INSERT INTO meta VALUES ('schema_version','" + schemaVersion + "')");
            s.execute("INSERT INTO meta VALUES ('data_date','2026-11-03')");
            s.execute("INSERT INTO meta VALUES ('credits','[\"Town data: U.S. Census Bureau\"]')");
            s.execute("CREATE TABLE place (source_id TEXT, name TEXT, search_name TEXT, type TEXT, state TEXT, "
                + "slug TEXT, latitude REAL, longitude REAL)");
            s.execute("INSERT INTO place VALUES ('2501701605','Ashland','ashland','town','MA','ashland-ma',42.25,-71.47)");
        }
        path.toFile().setReadOnly();
        return path;
    }

    @Test
    void notConfiguredWhenFileIsBlank() {
        GeoPlaceSource g = new GeoPlaceSource("");
        assertThat(g.isUp()).isFalse();
        assertThat(g.reason()).contains(GeoPlaceSource.Reason.NOT_CONFIGURED);
        assertThat(g.places()).isEmpty();
        assertThat(g.credits()).contains("ZIP data: GeoNames (geonames.org), CC BY 4.0");
    }

    @Test
    void notFoundWhenFileMissing(@TempDir Path dir) {
        GeoPlaceSource g = new GeoPlaceSource(dir.resolve("absent.sqlite").toString());
        assertThat(g.reason()).contains(GeoPlaceSource.Reason.NOT_FOUND);
    }

    @Test
    void wrongVersionIsRejected(@TempDir Path dir) throws Exception {
        Path f = writeFile(dir, "v2.sqlite", 2);
        GeoPlaceSource g = new GeoPlaceSource(f.toString());
        assertThat(g.isUp()).isFalse();
        assertThat(g.reason()).contains(GeoPlaceSource.Reason.WRONG_VERSION);
        assertThat(g.places()).isEmpty();
    }

    @Test
    void goodFileLoadsPlacesAndMeta(@TempDir Path dir) throws Exception {
        Path f = writeFile(dir, "geo-2026-11-03.sqlite", 1);
        GeoPlaceSource g = new GeoPlaceSource(f.toString());
        assertThat(g.isUp()).isTrue();
        assertThat(g.reason()).isEmpty();
        assertThat(g.dataDate()).contains("2026-11-03");
        assertThat(g.schemaVersion()).contains(1);
        assertThat(g.places()).hasSize(1);
        assertThat(g.bySlug("ashland-ma")).isPresent();
        assertThat(g.bySlug("nope")).isEmpty();
        // Credits from the file plus the always-present GeoNames line.
        assertThat(g.credits()).contains("Town data: U.S. Census Bureau",
            "ZIP data: GeoNames (geonames.org), CC BY 4.0");
    }

    @Test
    void readOnlyOpenDoesNotCreateSideFiles(@TempDir Path dir) throws Exception {
        Path f = writeFile(dir, "geo.sqlite", 1);
        new GeoPlaceSource(f.toString());
        assertThat(Files.exists(dir.resolve("geo.sqlite-wal"))).isFalse();
        assertThat(Files.exists(dir.resolve("geo.sqlite-shm"))).isFalse();
    }

    @Test
    void parsesJsonStringArray() {
        assertThat(GeoPlaceSource.parseJsonStringArray("[\"a\", \"b c\"]")).containsExactly("a", "b c");
        assertThat(GeoPlaceSource.parseJsonStringArray(null)).isEmpty();
    }
}
