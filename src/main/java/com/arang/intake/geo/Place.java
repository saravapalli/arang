package com.arang.intake.geo;

/**
 * One searchable place from the place file: for P2.1, a Massachusetts municipality (ARANG-SDD-P2-1 §1.3).
 *
 * @param sourceId   Census GEOID of the county subdivision
 * @param name       display name, Census suffixes removed (for example "Agawam")
 * @param searchName normalized name used for matching (lowercase, no accents or punctuation)
 * @param type       {@code city} or {@code town}; kept so villages can be added later without a schema change
 * @param state      two-letter state code
 * @param slug       stable URL name, for example {@code ashland-ma}
 * @param latitude   centre point latitude
 * @param longitude  centre point longitude
 */
public record Place(
    String sourceId,
    String name,
    String searchName,
    String type,
    String state,
    String slug,
    double latitude,
    double longitude) {
}
