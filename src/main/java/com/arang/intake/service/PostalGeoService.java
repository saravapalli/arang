package com.arang.intake.service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * Derives an approximate latitude/longitude from a postal code (REQ-13, DD-3).
 *
 * <p>Prototype implementation: a small bundled US ZIP-centroid lookup (public-domain
 * approximate centroids). An unknown ZIP yields no coordinates (publish still succeeds — the
 * postal code is present; coordinates are best-effort/approximate). A fuller dataset or a
 * geocoding service can replace this behind the same method without schema change.
 */
@Service
public class PostalGeoService {

    private record Centroid(BigDecimal lat, BigDecimal lon) {
    }

    // A tiny sample set for the prototype. Not exhaustive by design (DD-3).
    private static final Map<String, Centroid> US_ZIP_CENTROIDS = Map.of(
        "10001", new Centroid(new BigDecimal("40.750742"), new BigDecimal("-73.996567")),
        "10002", new Centroid(new BigDecimal("40.715888"), new BigDecimal("-73.986069")),
        "94103", new Centroid(new BigDecimal("37.772600"), new BigDecimal("-122.410800")),
        "60601", new Centroid(new BigDecimal("41.885600"), new BigDecimal("-87.621800")),
        "02108", new Centroid(new BigDecimal("42.357600"), new BigDecimal("-71.063800")),
        "98101", new Centroid(new BigDecimal("47.610400"), new BigDecimal("-122.334600")),
        "73301", new Centroid(new BigDecimal("30.266700"), new BigDecimal("-97.743100"))
    );

    /** @return approximate [lat, lon], or empty if the postal/country is unknown. */
    public Optional<BigDecimal[]> deriveCentroid(String postalCode, String countryCode) {
        if (postalCode == null || postalCode.isBlank()) {
            return Optional.empty();
        }
        String cc = (countryCode == null || countryCode.isBlank()) ? "US" : countryCode;
        if (!"US".equalsIgnoreCase(cc)) {
            return Optional.empty(); // international deferred (DD-3)
        }
        String zip5 = postalCode.trim();
        if (zip5.length() > 5) {
            zip5 = zip5.substring(0, 5); // ZIP+4 → 5-digit
        }
        Centroid c = US_ZIP_CENTROIDS.get(zip5);
        if (c == null) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal[] { c.lat(), c.lon() });
    }
}
