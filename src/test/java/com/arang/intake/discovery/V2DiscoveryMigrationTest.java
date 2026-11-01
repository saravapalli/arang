package com.arang.intake.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * TC-01 (ARANG-TTP-P2): after the V2 migration runs, the curated category vocabulary is seeded,
 * `user.external_ref` exists as a nullable UNIQUE column, and the discovery composite index exists.
 * Proves SDD-P2 T1 / REQ-17.
 */
@SpringBootTest
@ActiveProfiles("test")
class V2DiscoveryMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    private static final List<String> EXPECTED_CATEGORIES = List.of(
        "music", "food", "family", "arts", "outdoor", "sports", "workshop", "nightlife",
        "theatre", "film", "community", "wellness", "market", "offer", "other");

    @Test
    void seedsTheFifteenCuratedCategories() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM category", Integer.class);
        assertThat(count).isEqualTo(15);

        List<String> names = jdbc.queryForList("SELECT name FROM category", String.class);
        assertThat(names).containsExactlyInAnyOrderElementsOf(EXPECTED_CATEGORIES);
    }

    @Test
    void addsNullableExternalRefColumnToUser() {
        // H2 information_schema (identifiers are lower-cased by DATABASE_TO_LOWER).
        Integer cols = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE LOWER(table_name) = 'user' AND LOWER(column_name) = 'external_ref'",
            Integer.class);
        assertThat(cols).as("user.external_ref column exists").isEqualTo(1);

        String nullable = jdbc.queryForObject(
            "SELECT is_nullable FROM information_schema.columns "
                + "WHERE LOWER(table_name) = 'user' AND LOWER(column_name) = 'external_ref'",
            String.class);
        assertThat(nullable.toUpperCase()).as("external_ref is nullable").isEqualTo("YES");
    }

    @Test
    void addsDiscoveryCompositeIndex() {
        Integer idx = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.indexes "
                + "WHERE LOWER(index_name) = 'idx_event_pub_starts'",
            Integer.class);
        assertThat(idx).as("idx_event_pub_starts exists").isEqualTo(1);
    }
}
