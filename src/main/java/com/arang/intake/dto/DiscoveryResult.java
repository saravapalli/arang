package com.arang.intake.dto;

import java.util.List;

/**
 * A single discovery result for the consumer SPA (ARANG-SDD-P2 §2). Pin fields {@code lat}/{@code lng}
 * are null when the event has a postal code but no derived centroid (REQ-07: listed, not pinned).
 */
public record DiscoveryResult(
    long id,
    String title,
    String startsAt,
    String location,
    String postalCode,
    Double lat,
    Double lng,
    List<String> categories,
    String image,
    String price
) {}
