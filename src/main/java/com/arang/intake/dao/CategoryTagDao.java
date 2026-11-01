package com.arang.intake.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Writes event→category and event→tag links during enrichment (ARANG-SDD-P2 §2, REQ-15/16/17).
 * Categories are validated against the curated {@code category} vocabulary (unknown names are
 * dropped — the LLM is instructed to use the list). Tags are open: find-or-mint in {@code tag}.
 */
@Repository
public class CategoryTagDao {

    private final NamedParameterJdbcTemplate jdbc;

    public CategoryTagDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Link an event to curated categories by name. Unknown names are skipped (REQ-15/17). */
    public void linkCategories(long eventId, List<String> categoryNames) {
        for (String name : categoryNames) {
            Optional<Long> cid = categoryId(name);
            cid.ifPresent(id -> jdbc.getJdbcOperations().update(
                "INSERT IGNORE INTO event_category (event_id, category_id) VALUES (?, ?)",
                eventId, id));
        }
    }

    /** The curated category names currently on an event. */
    public List<String> categoriesFor(long eventId) {
        return jdbc.query(
            "SELECT c.name FROM event_category ec JOIN category c ON c.id = ec.category_id " +
            "WHERE ec.event_id = :id ORDER BY c.name",
            Map.of("id", eventId),
            (rs, n) -> rs.getString("name"));
    }

    /** Remove one curated category from an event (REQ-18). No-op if not present. */
    public void unlinkCategory(long eventId, String categoryName) {
        categoryId(categoryName).ifPresent(id -> jdbc.getJdbcOperations().update(
            "DELETE FROM event_category WHERE event_id = ? AND category_id = ?", eventId, id));
    }

    /** All curated category names (the vocabulary), for the review-page picker. */
    public List<String> allCategoryNames() {
        return jdbc.getJdbcOperations().query(
            "SELECT name FROM category ORDER BY name", (rs, n) -> rs.getString("name"));
    }

    /** Link an event to open tags by name, minting the tag row if new (REQ-16). */
    public void linkTags(long eventId, List<String> tagNames) {
        for (String name : tagNames) {
            long tid = findOrMintTag(name);
            jdbc.getJdbcOperations().update(
                "INSERT IGNORE INTO event_tag (event_id, tag_id) VALUES (?, ?)", eventId, tid);
        }
    }

    private Optional<Long> categoryId(String name) {
        List<Long> ids = jdbc.query("SELECT id FROM category WHERE name = :n",
            Map.of("n", name), (rs, n) -> rs.getLong("id"));
        return ids.stream().findFirst();
    }

    private long findOrMintTag(String name) {
        List<Long> ids = jdbc.query("SELECT id FROM tag WHERE name = :n",
            Map.of("n", name), (rs, n) -> rs.getLong("id"));
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update("INSERT INTO tag (name) VALUES (:n)",
            new org.springframework.jdbc.core.namedparam.MapSqlParameterSource("n", name),
            keys, new String[] { "id" });
        return keys.getKey().longValue();
    }
}
