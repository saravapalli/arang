package com.arang.intake.dto;

import java.util.List;

/**
 * The discovery response envelope (ARANG-SDD-P2 §2). Carries the resolved window/area, a
 * {@code truncated} flag when the result cap is hit, {@code interestFilterMatchedNothing} for the
 * empty-interest fallback (REQ-14), and the result list.
 */
public record DiscoveryEnvelope(
    Window window,
    Area area,
    boolean truncated,
    boolean interestFilterMatchedNothing,
    List<DiscoveryResult> results
) {
    public record Window(String from, String to) {}
    public record Area(String postal, Integer radiusMiles) {}
}
