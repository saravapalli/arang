package com.arang.intake.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * WhatsApp integration configuration, bound from {@code arang.whatsapp.*}.
 *
 * <ul>
 *   <li>{@code verifyToken} — GET webhook verification handshake (REQ-01).</li>
 *   <li>{@code accessToken} / {@code phoneNumberId} — Cloud API send credentials (REQ-08). When
 *       {@code accessToken} is blank, a no-op logging client is used instead of real sends.</li>
 *   <li>{@code publicBaseUrl} — base for the review link in replies (your tunnel URL).</li>
 *   <li>{@code lowConfidenceThreshold} — fields below this are flagged ⚠️ in the reply.</li>
 * </ul>
 * Secrets come from the environment and are never committed.
 */
@ConfigurationProperties(prefix = "arang.whatsapp")
public class WebhookProperties {

    private String verifyToken = "";
    private String accessToken = "";
    private String phoneNumberId = "";
    private String publicBaseUrl = "http://localhost:8080";
    private BigDecimal lowConfidenceThreshold = new BigDecimal("0.5");

    public String getVerifyToken() {
        return verifyToken;
    }

    public void setVerifyToken(String verifyToken) {
        this.verifyToken = verifyToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getPhoneNumberId() {
        return phoneNumberId;
    }

    public void setPhoneNumberId(String phoneNumberId) {
        this.phoneNumberId = phoneNumberId;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public BigDecimal getLowConfidenceThreshold() {
        return lowConfidenceThreshold;
    }

    public void setLowConfidenceThreshold(BigDecimal lowConfidenceThreshold) {
        this.lowConfidenceThreshold = lowConfidenceThreshold;
    }
}
