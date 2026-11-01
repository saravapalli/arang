package com.arang.intake.dao;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.arang.intake.dao.InterestDao.InterestRow;

/**
 * Consumer interests (ARANG-SDD-P2 §1, REQ-10). Stored in the existing {@code interest} table
 * keyed by {@code user_id}. This slice handles category interests (the controlled spine);
 * {@code weight} is unused in the MVP.
 */
@Repository
public class InterestDao {

    public record InterestRow(long userId, long categoryId, String categoryName) {}

    private final NamedParameterJdbcTemplate jdbc;

    public InterestDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The consumer's saved category interests (names). */
    public List<String> findCategoryNames(long userId) {
        return jdbc.query(
            "SELECT c.name FROM interest i JOIN category c ON c.id = i.category_id " +
            "WHERE i.user_id = :uid AND i.category_id IS NOT NULL ORDER BY c.name",
            Map.of("uid", userId),
            (rs, n) -> rs.getString("name"));
    }

    /** Replace the consumer's category interests with the given category ids (idempotent save). */
    public void replaceCategories(long userId, List<Long> categoryIds) {
        jdbc.update("DELETE FROM interest WHERE user_id = :uid AND category_id IS NOT NULL",
            Map.of("uid", userId));
        for (Long cid : categoryIds) {
            jdbc.update(
                "INSERT INTO interest (user_id, category_id, weight) VALUES (:uid, :cid, 1.000)",
                Map.of("uid", userId, "cid", cid));
        }
    }

    /** Resolve a category name to its id, or empty if not in the curated vocabulary (REQ-11). */
    public java.util.Optional<Long> categoryIdByName(String name) {
        List<Long> ids = jdbc.query(
            "SELECT id FROM category WHERE name = :name",
            Map.of("name", name),
            (rs, n) -> rs.getLong("id"));
        return ids.stream().findFirst();
    }
}
