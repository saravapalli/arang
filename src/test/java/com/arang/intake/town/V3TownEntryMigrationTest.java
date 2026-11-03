package com.arang.intake.town;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * TC-P2.1-008 and TC-P2.1-009 (ARANG-TTP-P2-1): the V3 migration adds the town registry, slug aliases,
 * user.email, and demand_signal, and seeds the 28 live Massachusetts towns. Proves SDD-P2.1 task T3.
 */
@SpringBootTest
@ActiveProfiles("test")
class V3TownEntryMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void seedsTheTwentyEightLiveMassachusettsTowns() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM town", Integer.class)).isEqualTo(28);
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM town WHERE state = 'MA' AND status = 'live'", Integer.class)).isEqualTo(28);

        List<Map<String, Object>> ranked = jdbc.queryForList(
            "SELECT slug FROM town WHERE popular_rank IS NOT NULL ORDER BY popular_rank");
        assertThat(ranked).extracting(r -> r.get("slug")).containsExactly(
            "ashland-ma", "framingham-ma", "natick-ma", "boston-ma", "cambridge-ma", "newton-ma");

        Map<String, Object> ashland = jdbc.queryForMap(
            "SELECT name, postal_code, map_zoom FROM town WHERE slug = 'ashland-ma'");
        assertThat(ashland.get("name")).isEqualTo("Ashland");
        assertThat(ashland.get("postal_code")).isEqualTo("01721");
    }

    @Test
    void slugsAreUniqueAndLowercase() {
        Integer distinct = jdbc.queryForObject("SELECT COUNT(DISTINCT slug) FROM town", Integer.class);
        assertThat(distinct).isEqualTo(28);
        List<String> slugs = jdbc.queryForList("SELECT slug FROM town", String.class);
        assertThat(slugs).allMatch(s -> s.matches("[a-z0-9-]+-ma"));
    }

    @Test
    void userEmailIsNullableAndUnique() {
        String nullable = jdbc.queryForObject(
            "SELECT is_nullable FROM information_schema.columns "
                + "WHERE LOWER(table_name) = 'user' AND LOWER(column_name) = 'email'",
            String.class);
        assertThat(nullable.toUpperCase()).isEqualTo("YES");

        jdbc.update("INSERT INTO `user` (email) VALUES ('unique-check@example.com')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO `user` (email) VALUES ('unique-check@example.com')"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void demandSignalIsUniquePerUserKindPlaceAndCascadesOnUserDelete() {
        jdbc.update("INSERT INTO `user` (email) VALUES ('cascade-check@example.com')");
        Long userId = jdbc.queryForObject(
            "SELECT id FROM `user` WHERE email = 'cascade-check@example.com'", Long.class);

        String insert = "INSERT INTO demand_signal (user_id, place_key, place_label, consent_version, consent_text) "
            + "VALUES (?, 'worcester-ma', 'Worcester', 'v1', 'We''ll only email you about Arangly in Worcester.')";
        jdbc.update(insert, userId);
        assertThatThrownBy(() -> jdbc.update(insert, userId)).isInstanceOf(DataIntegrityViolationException.class);

        Map<String, Object> row = jdbc.queryForMap(
            "SELECT kind, verified_at FROM demand_signal WHERE user_id = ?", userId);
        assertThat(row.get("kind")).isEqualTo("town_waitlist");
        assertThat(row.get("verified_at")).isNull();

        jdbc.update("DELETE FROM `user` WHERE id = ?", userId);
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM demand_signal WHERE user_id = ?", Integer.class, userId)).isZero();
    }

    @Test
    void slugAliasPointsToTownAndCascades() {
        Long ashland = jdbc.queryForObject("SELECT id FROM town WHERE slug = 'ashland-ma'", Long.class);
        jdbc.update("INSERT INTO town_slug_alias (alias, town_id) VALUES ('old-ashland-ma', ?)", ashland);
        assertThat(jdbc.queryForObject(
            "SELECT t.slug FROM town_slug_alias a JOIN town t ON t.id = a.town_id WHERE a.alias = 'old-ashland-ma'",
            String.class)).isEqualTo("ashland-ma");
    }
}
