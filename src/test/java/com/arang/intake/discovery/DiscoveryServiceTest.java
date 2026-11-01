package com.arang.intake.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.dao.EventDao;
import com.arang.intake.dto.DiscoveryResult;
import com.arang.intake.model.Event;
import com.arang.intake.service.DiscoveryService;

/**
 * S-B DiscoveryService (ARANG-TTP-P2): TC-02 (published + window), TC-04 (area: postal + radius),
 * TC-05 (from>to rejected), TC-06 (unresolved location empty), TC-07 (pin vs list), TC-08 (category OR).
 */
@SpringBootTest
@ActiveProfiles("test")
class DiscoveryServiceTest {

    @Autowired DiscoveryService discovery;
    @Autowired EventDao events;
    @Autowired JdbcTemplate jdbc;

    // This-week window around a fixed in-window instant.
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 10, 26, 0, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 11, 1, 23, 59, 59);
    private static final LocalDateTime IN = LocalDateTime.of(2026, 10, 30, 20, 0, 0);
    private static final LocalDateTime OUT = LocalDateTime.of(2026, 12, 1, 20, 0, 0);

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM event_category");
        events.deleteAll();
    }

    private Event ev(String title, String status, LocalDateTime startsAt, String postal,
                     String lat, String lng) {
        Event e = new Event();
        e.setTitle(title);
        e.setStatus(status);
        e.setSourceType("text");
        e.setOriginType("user");
        e.setStartsAt(startsAt);
        e.setPostalCode(postal);
        if (lat != null) e.setLatitude(new BigDecimal(lat));
        if (lng != null) e.setLongitude(new BigDecimal(lng));
        return events.insert(e);
    }

    private void tag(long eventId, String categoryName) {
        Long cid = jdbc.queryForObject("SELECT id FROM category WHERE name = ?", Long.class, categoryName);
        jdbc.update("INSERT INTO event_category (event_id, category_id) VALUES (?, ?)", eventId, cid);
    }

    @Test
    void returnsOnlyPublishedInWindow() { // TC-02
        ev("Published in-window", "published", IN, "01721", "42.2459", "-71.4665");
        ev("Draft in-window", "draft", IN, "01721", "42.2459", "-71.4665");
        ev("Published out-of-window", "published", OUT, "01721", "42.2459", "-71.4665");

        List<DiscoveryResult> r = discovery.find(FROM, TO, null, null);
        assertThat(r).extracting(DiscoveryResult::title).containsExactly("Published in-window");
    }

    @Test
    void areaMatchesByPostalAndByRadius() { // TC-04
        // Ashland 01721 centroid ≈ 42.2612,-71.4634. Same-postal match:
        ev("Same postal", "published", IN, "01721", null, null);
        // Different postal but within 15 mi (a point ~5 mi away):
        ev("Within radius", "published", IN, "99999", "42.3300", "-71.4600");
        // Far away (NYC, ~190 mi):
        ev("Too far", "published", IN, "10001", "40.7507", "-73.9966");

        List<DiscoveryResult> r = discovery.find(FROM, TO, "01721", null);
        assertThat(r).extracting(DiscoveryResult::title)
            .contains("Same postal", "Within radius")
            .doesNotContain("Too far");
    }

    @Test
    void rejectsInvertedWindow() { // TC-05
        assertThatThrownBy(() -> discovery.find(TO, FROM, null, null))
            .isInstanceOf(DiscoveryService.InvalidWindowException.class);
    }

    @Test
    void unresolvedLocationReturnsEmpty() { // TC-06
        ev("Ashland event", "published", IN, "01721", "42.2459", "-71.4665");
        // A postal with no centroid AND no event sharing it → empty (not an error).
        List<DiscoveryResult> r = discovery.find(FROM, TO, "00000", null);
        assertThat(r).isEmpty();
    }

    @Test
    void pinnableWhenCoordsPresentListOnlyWhenNot() { // TC-07
        ev("Pinnable", "published", IN, "01721", "42.2459", "-71.4665");
        ev("List only", "published", IN, "01721", null, null);
        List<DiscoveryResult> r = discovery.find(FROM, TO, null, null);
        DiscoveryResult pin = r.stream().filter(x -> x.title().equals("Pinnable")).findFirst().orElseThrow();
        DiscoveryResult list = r.stream().filter(x -> x.title().equals("List only")).findFirst().orElseThrow();
        assertThat(pin.lat()).isNotNull();
        assertThat(pin.lng()).isNotNull();
        assertThat(list.lat()).isNull();
        assertThat(list.lng()).isNull();
    }

    @Test
    void categoryFilterIsOrSemantics() { // TC-08
        Event music = ev("Music event", "published", IN, "01721", "42.2459", "-71.4665");
        Event food = ev("Food event", "published", IN, "01721", "42.2459", "-71.4665");
        ev("Uncategorized", "published", IN, "01721", "42.2459", "-71.4665");
        tag(music.getId(), "music");
        tag(food.getId(), "food");

        List<DiscoveryResult> r = discovery.find(FROM, TO, null, List.of("music", "food"));
        assertThat(r).extracting(DiscoveryResult::title)
            .contains("Music event", "Food event")
            .doesNotContain("Uncategorized");
    }
}
