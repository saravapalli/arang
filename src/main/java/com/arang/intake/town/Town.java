package com.arang.intake.town;

/**
 * A town Arangly serves, from the {@code town} table (ARANG-SDD-P2-1 §1.1). {@code status} is data
 * ('live' | 'hidden'); "coming soon" is derived, not stored.
 *
 * @param slug        stable URL name, for example {@code ashland-ma}
 * @param name        display name
 * @param state       two-letter state code
 * @param postalCode  primary ZIP, used by the existing discovery query
 * @param latitude    centre point latitude
 * @param longitude   centre point longitude
 * @param mapZoom      initial map zoom
 * @param status      'live' or 'hidden'
 * @param popularRank non-null ⇒ shown as a "Popular" chip, lowest first
 */
public record Town(
    String slug,
    String name,
    String state,
    String postalCode,
    double latitude,
    double longitude,
    int mapZoom,
    String status,
    Integer popularRank) {

    public boolean isLive() {
        return "live".equals(status);
    }
}
