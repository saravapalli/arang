package com.arang.intake.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * The universal unit (SDD §1 {@code event}). Plain POJO; persisted by {@code EventDao} with
 * explicit SQL. Most typed-core fields stay null in P1 (filled on the review page or a later LLM
 * pass).
 */
public class Event {

    private Long id;
    private Long eventTypeId;
    private Long businessId;
    private String title;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private String location;
    private String postalCode;
    private String countryCode = "US";
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String sourceType;
    private String rawMessage;
    private String mediaRef;
    private BigDecimal extractedConfidence;
    private String status = "draft";
    private String originType;
    private Long submittedBy;
    private Long ingestionSourceId;
    private Long apiClientId;
    private String editToken;
    private LocalDateTime editTokenExpiresAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventTypeId() {
        return eventTypeId;
    }

    public void setEventTypeId(Long eventTypeId) {
        this.eventTypeId = eventTypeId;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getMediaRef() {
        return mediaRef;
    }

    public void setMediaRef(String mediaRef) {
        this.mediaRef = mediaRef;
    }

    public BigDecimal getExtractedConfidence() {
        return extractedConfidence;
    }

    public void setExtractedConfidence(BigDecimal extractedConfidence) {
        this.extractedConfidence = extractedConfidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOriginType() {
        return originType;
    }

    public void setOriginType(String originType) {
        this.originType = originType;
    }

    public Long getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(Long submittedBy) {
        this.submittedBy = submittedBy;
    }

    public Long getIngestionSourceId() {
        return ingestionSourceId;
    }

    public void setIngestionSourceId(Long ingestionSourceId) {
        this.ingestionSourceId = ingestionSourceId;
    }

    public Long getApiClientId() {
        return apiClientId;
    }

    public void setApiClientId(Long apiClientId) {
        this.apiClientId = apiClientId;
    }

    public String getEditToken() {
        return editToken;
    }

    public void setEditToken(String editToken) {
        this.editToken = editToken;
    }

    public LocalDateTime getEditTokenExpiresAt() {
        return editTokenExpiresAt;
    }

    public void setEditTokenExpiresAt(LocalDateTime editTokenExpiresAt) {
        this.editTokenExpiresAt = editTokenExpiresAt;
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

    public void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }
}
