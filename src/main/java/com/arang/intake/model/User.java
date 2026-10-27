package com.arang.intake.model;

import java.time.Instant;

/**
 * Identity ONLY (SDD §1 {@code user}). Plain POJO; persisted by {@code UserDao} with explicit SQL.
 * No {@code user_type} column — roles live in {@code user_role} (not populated in P1).
 */
public class User {

    private Long id;
    private String phoneNumber;
    private String name;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public User() {
    }

    public User(String phoneNumber, String name) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
