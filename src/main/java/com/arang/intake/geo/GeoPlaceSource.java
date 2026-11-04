package com.arang.intake.geo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Loads the read-only place file once at startup and holds it in memory (ARANG-SDD-P2-1 §3.4, task T4).
 *
 * <p>Rules:
 * <ul>
 *   <li>The file is opened read-only ({@code mode=ro}), read, and closed. The app never writes to it.</li>
 *   <li>This class never throws at startup. A missing or wrong file gives status {@code UNAVAILABLE} with a reason
 *       from a fixed list, so the rest of the site keeps working (REQ-P2.1-18).</li>
 *   <li>It is not a {@code DataSource} bean, so Spring's MySQL data source setup is unaffected (DD-P2.1-9).</li>
 *   <li>The in-memory data never changes; a new file needs a restart.</li>
 * </ul>
 *
 * <p>Note for deployment: the SQLite driver unpacks a native library into a temp folder at startup. On a read-only
 * root filesystem, set the JVM property {@code org.sqlite.tmpdir} to a writable folder (SDD U4).
 */
@Component
public class GeoPlaceSource {

    private static final Logger log = LoggerFactory.getLogger(GeoPlaceSource.class);

    /** The schema version this code understands. The build script writes the same number. */
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    /** Shown even without a place file: the bundled ZIP data for event map pins comes from GeoNames (CC BY 4.0). */
    public static final List<String> BASELINE_CREDITS = List.of("ZIP data: GeoNames (geonames.org), CC BY 4.0");

    /** Health status of the place data. */
    public enum Status { UP, UNAVAILABLE }

    /** Fixed reasons for {@link Status#UNAVAILABLE}. Never a path or exception text: /health is public. */
    public enum Reason { NOT_CONFIGURED, NOT_FOUND, UNREADABLE, WRONG_VERSION }

    private static final Pattern JSON_STRING = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final Status status;
    private final Reason reason;
    private final String dataDate;
    private final Integer schemaVersion;
    private final List<String> credits;
    private final List<Place> places;
    private final Map<String, Place> bySlug;

    @Autowired
    public GeoPlaceSource(GeoProperties props) {
        this(props.getFile());
    }

    /** Loads from the given path; {@code null} or blank means not configured. Package-visible for tests. */
    GeoPlaceSource(String file) {
        Loaded loaded = load(file);
        this.status = loaded.reason == null ? Status.UP : Status.UNAVAILABLE;
        this.reason = loaded.reason;
        this.dataDate = loaded.dataDate;
        this.schemaVersion = loaded.schemaVersion;
        this.credits = Collections.unmodifiableList(loaded.credits);
        this.places = Collections.unmodifiableList(loaded.places);
        Map<String, Place> index = new HashMap<>();
        for (Place p : loaded.places) {
            index.put(p.slug(), p);
        }
        this.bySlug = Collections.unmodifiableMap(index);
        if (status == Status.UP) {
            log.info("Place data loaded: {} places, data date {}", places.size(), dataDate);
        } else {
            log.warn("Place data unavailable ({}); town search is off, other pages keep working", reason);
        }
    }

    public Status status() {
        return status;
    }

    public boolean isUp() {
        return status == Status.UP;
    }

    /** The reason when unavailable, else empty. */
    public Optional<Reason> reason() {
        return Optional.ofNullable(reason);
    }

    /** The file's data date (YYYY-MM-DD), or empty when unavailable. */
    public Optional<String> dataDate() {
        return Optional.ofNullable(dataDate);
    }

    public Optional<Integer> schemaVersion() {
        return Optional.ofNullable(schemaVersion);
    }

    /** Credit lines to show on the site. Always includes the GeoNames line. */
    public List<String> credits() {
        return credits;
    }

    /** All places, or an empty list when unavailable. */
    public List<Place> places() {
        return places;
    }

    public Optional<Place> bySlug(String slug) {
        return Optional.ofNullable(bySlug.get(slug));
    }

    // ---------------------------------------------------------------------------------------------

    private record Loaded(Reason reason, String dataDate, Integer schemaVersion, List<String> credits,
                          List<Place> places) {
        static Loaded unavailable(Reason reason) {
            return new Loaded(reason, null, null, new ArrayList<>(BASELINE_CREDITS), List.of());
        }
    }

    private static Loaded load(String file) {
        if (file == null || file.isBlank()) {
            return Loaded.unavailable(Reason.NOT_CONFIGURED);
        }
        Path path = Path.of(file.trim());
        if (!Files.isRegularFile(path)) {
            return Loaded.unavailable(Reason.NOT_FOUND);
        }
        if (!Files.isReadable(path)) {
            return Loaded.unavailable(Reason.UNREADABLE);
        }
        String url = "jdbc:sqlite:file:" + path.toAbsolutePath() + "?mode=ro";
        try (Connection con = DriverManager.getConnection(url); Statement st = con.createStatement()) {
            Map<String, String> meta = new HashMap<>();
            try (ResultSet rs = st.executeQuery("SELECT key, value FROM meta")) {
                while (rs.next()) {
                    meta.put(rs.getString(1), rs.getString(2));
                }
            }
            Integer version = parseInt(meta.get("schema_version"));
            if (version == null || version != SUPPORTED_SCHEMA_VERSION) {
                return Loaded.unavailable(Reason.WRONG_VERSION);
            }
            List<Place> places = new ArrayList<>();
            try (ResultSet rs = st.executeQuery(
                    "SELECT source_id, name, search_name, type, state, slug, latitude, longitude "
                        + "FROM place ORDER BY name")) {
                while (rs.next()) {
                    places.add(new Place(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getDouble(7), rs.getDouble(8)));
                }
            }
            List<String> credits = new ArrayList<>(parseJsonStringArray(meta.get("credits")));
            for (String baseline : BASELINE_CREDITS) {
                if (!credits.contains(baseline)) {
                    credits.add(baseline);
                }
            }
            return new Loaded(null, meta.get("data_date"), version, credits, places);
        } catch (Exception | LinkageError e) {
            // LinkageError covers a native SQLite library that cannot load (for example no writable temp folder).
            log.warn("Place file could not be read: {}", e.getClass().getSimpleName());
            return Loaded.unavailable(Reason.UNREADABLE);
        }
    }

    private static Integer parseInt(String s) {
        try {
            return s == null ? null : Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Minimal reader for the build script's JSON list of strings, for example ["a", "b"]. */
    static List<String> parseJsonStringArray(String json) {
        List<String> out = new ArrayList<>();
        if (json == null) {
            return out;
        }
        Matcher m = JSON_STRING.matcher(json);
        while (m.find()) {
            out.add(m.group(1).replace("\\\"", "\"").replace("\\\\", "\\"));
        }
        return out;
    }
}
