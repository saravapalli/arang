package com.arang.intake.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Derives an approximate latitude/longitude from a US postal code (REQ-13, DD-3 / DD-P2-3).
 *
 * <p>Backed by a bundled ZIP-centroid dataset ({@code /geo/zip-centroids.csv}) covering every ZIP
 * within ~100 miles of Ashland, MA (~1,365 ZIPs, derived from the public-domain GeoNames US set).
 * This is the coverage the consumer discovery MVP needs: Ashland and all surrounding towns.
 * An unknown ZIP (outside the bundled region) yields no coordinates. A nationwide dataset or a
 * live geocoding service can replace the loaded map behind the same method, no schema change.
 */
@Service
public class PostalGeoService {

    private static final Logger log = LoggerFactory.getLogger(PostalGeoService.class);
    private static final String DATASET = "geo/zip-centroids.csv";

    private record Centroid(BigDecimal lat, BigDecimal lon) {}

    private final Map<String, Centroid> centroids = new HashMap<>();

    public PostalGeoService() {
        load();
    }

    private void load() {
        ClassPathResource res = new ClassPathResource(DATASET);
        if (!res.exists()) {
            log.warn("ZIP centroid dataset {} not found — geocoding will return empty for all ZIPs", DATASET);
            return;
        }
        try (InputStream in = res.getInputStream();
             BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line = r.readLine(); // header: postal,lat,lng
            int n = 0;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] p = line.split(",");
                if (p.length < 3) continue;
                try {
                    centroids.put(p[0].trim(), new Centroid(new BigDecimal(p[1].trim()), new BigDecimal(p[2].trim())));
                    n++;
                } catch (NumberFormatException ignore) { /* skip malformed row */ }
            }
            log.info("Loaded {} ZIP centroids (within ~100mi of Ashland, MA) from {}", n, DATASET);
        } catch (Exception e) {
            log.warn("Failed to load ZIP centroid dataset {}: {}", DATASET, e.toString());
        }
    }

    /** @return approximate [lat, lon], or empty if the ZIP is outside the covered region. */
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
        Centroid c = centroids.get(zip5);
        return (c == null) ? Optional.empty() : Optional.of(new BigDecimal[] { c.lat(), c.lon() });
    }

    /** Count of loaded ZIP centroids (for diagnostics/tests). */
    public int size() {
        return centroids.size();
    }
}
