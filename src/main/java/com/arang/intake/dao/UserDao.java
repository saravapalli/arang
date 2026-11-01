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
            "SELECT id, phone_number, name, external_ref FROM `user` WHERE phone_number = :phone",
            Map.of("phone", phoneNumber), MAPPER);
        return found.stream().findFirst();
    }

    /** Find a web consumer by their cookie id (REQ-12, DD-P2-2). */
    public Optional<User> findByExternalRef(String externalRef) {
        List<User> found = jdbc.query(
            "SELECT id, phone_number, name, external_ref FROM `user` WHERE external_ref = :ref",
            Map.of("ref", externalRef), MAPPER);
        return found.stream().findFirst();
    }

    public long count() {
        Long n = jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM `user`", Long.class);
        return n == null ? 0 : n;
    }

    public void deleteAll() {
        jdbc.getJdbcTemplate().update("DELETE FROM `user`");
    }
}
