package com.arang.intake.dto;

import java.util.List;
import java.util.Map;

/**
 * Full event detail for the consumer SPA detail view (ARANG-SDD-P2 §2, REQ-09).
 * Published events only; a draft/unknown id yields 404 at the controller.
 */
public record EventDetail(
    long id,
    String title,
    String startsAt,
    String endsAt,
    String location,
    String postalCode,
    Double lat,
    Double lng,
    List<String> categories,
    List<String> tags,
    String image,
    Map<String, String> attributes
) {}
