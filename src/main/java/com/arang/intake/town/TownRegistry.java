package com.arang.intake.town;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads the {@code town} coverage registry (ARANG-SDD-P2-1 §2, task T5). Live status is data, so these are
 * cache-free reads: a town going live or hidden takes effect on the next read (the page cache in §4.5 bounds how
 * soon a visitor sees it). Read-only; the operator changes rows directly (runbook in T19).
 */
@Repository
public class TownRegistry {

    private static final RowMapper<Town> MAPPER = (rs, n) -> new Town(
        rs.getString("slug"), rs.getString("name"), rs.getString("state"), rs.getString("postal_code"),
        rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getInt("map_zoom"), rs.getString("status"),
        (Integer) rs.getObject("popular_rank"));

    private static final String COLS =
        "slug, name, state, postal_code, latitude, longitude, map_zoom, status, popular_rank";

    private final NamedParameterJdbcTemplate jdbc;

    public TownRegistry(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** All live towns, popular ones first (by rank), then by name. */
    public List<Town> liveTowns() {
        return jdbc.query(
            "SELECT " + COLS + " FROM town WHERE status = 'live' "
                + "ORDER BY popular_rank IS NULL, popular_rank, name",
            MAPPER);
    }

    /** The slugs of all live towns. */
    public Set<String> liveSlugs() {
        return liveTowns().stream().map(Town::slug).collect(Collectors.toUnmodifiableSet());
    }

    /** A town by slug, live or hidden, or empty if unknown. */
    public Optional<Town> bySlug(String slug) {
        return jdbc.query("SELECT " + COLS + " FROM town WHERE slug = :slug", Map.of("slug", slug), MAPPER)
            .stream().findFirst();
    }

    /** The current slug a slug alias points to, or empty if the alias is unknown. */
    public Optional<String> resolveAlias(String alias) {
        return jdbc.query(
            "SELECT t.slug FROM town_slug_alias a JOIN town t ON t.id = a.town_id WHERE a.alias = :alias",
            Map.of("alias", alias), (rs, n) -> rs.getString("slug")).stream().findFirst();
    }
}
