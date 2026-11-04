package com.arang.intake.dao;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.arang.intake.model.User;

/** User identity DAO (REQ-15) — explicit SQL. */
@Repository
public class UserDao {

    private static final RowMapper<User> MAPPER = (rs, n) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setPhoneNumber(rs.getString("phone_number"));
        u.setName(rs.getString("name"));
        u.setExternalRef(rs.getString("external_ref"));
        u.setEmail(rs.getString("email"));
        return u;
    };

    private final NamedParameterJdbcTemplate jdbc;

    public UserDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Insert and populate the generated id on the POJO. */
    public User insert(User u) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update("""
            INSERT INTO `user` (phone_number, name, external_ref, created_at, updated_at)
            VALUES (:phone, :name, :externalRef, :createdAt, :updatedAt)
            """,
            new MapSqlParameterSource()
                .addValue("phone", u.getPhoneNumber())
                .addValue("name", u.getName())
                .addValue("externalRef", u.getExternalRef())
                .addValue("createdAt", Timestamp.from(Instant.now()))
                .addValue("updatedAt", Timestamp.from(Instant.now())),
            keys, new String[] { "id" });
        u.setId(keys.getKey().longValue());
        return u;
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        List<User> found = jdbc.query(
            "SELECT id, phone_number, name, external_ref, email FROM `user` WHERE phone_number = :phone",
            Map.of("phone", phoneNumber), MAPPER);
        return found.stream().findFirst();
    }

    /** Find a web consumer by their cookie id (REQ-12, DD-P2-2). */
    public Optional<User> findByExternalRef(String externalRef) {
        List<User> found = jdbc.query(
            "SELECT id, phone_number, name, external_ref, email FROM `user` WHERE external_ref = :ref",
            Map.of("ref", externalRef), MAPPER);
        return found.stream().findFirst();
    }

    /** Find a consumer by their email (lowercase), or empty (ARANG-SDD-P2-1 §4.3, T9). */
    public Optional<User> findByEmail(String email) {
        List<User> found = jdbc.query(
            "SELECT id, phone_number, name, external_ref, email FROM `user` WHERE email = :email",
            Map.of("email", email), MAPPER);
        return found.stream().findFirst();
    }

    /**
     * Find-or-create a user by email in one atomic statement, race-safe on MySQL (ARANG-SDD-P2-1 DD-P2.1-12).
     * {@code ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)} keeps the existing id when the email is already
     * present, so two concurrent requests end up with one row. The email must already be lowercased.
     * {@code INSERT IGNORE}-style conflicts leave {@code updated_at} unchanged (the update is a no-op on id).
     */
    public long findOrCreateByEmail(String lowercaseEmail) {
        jdbc.update("""
            INSERT INTO `user` (email, created_at, updated_at)
            VALUES (:email, :now, :now)
            ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)
            """,
            new MapSqlParameterSource()
                .addValue("email", lowercaseEmail)
                .addValue("now", Timestamp.from(Instant.now())));
        Long id = jdbc.getJdbcTemplate().queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id == null ? 0 : id;
    }

    public long count() {
        Long n = jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM `user`", Long.class);
        return n == null ? 0 : n;
    }

    public void deleteAll() {
        jdbc.getJdbcTemplate().update("DELETE FROM `user`");
    }
}
