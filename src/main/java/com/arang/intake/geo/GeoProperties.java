package com.arang.intake.geo;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Place-file settings, bound from {@code arang.geo.*} (ARANG-SDD-P2-1 §3.4, task T4).
 *
 * <p>{@code file} is the path to the read-only place file built offline by
 * {@code scripts/geo/build_geo_sqlite.py}. Empty means "not configured": the app still starts, and town
 * search reports that place data is unavailable (REQ-P2.1-18).
 */
@ConfigurationProperties(prefix = "arang.geo")
public class GeoProperties {

    /** Path to {@code geo-YYYY-MM-DD.sqlite}. Env: {@code ARANG_GEO_FILE}. */
    private String file = "";

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }
}
