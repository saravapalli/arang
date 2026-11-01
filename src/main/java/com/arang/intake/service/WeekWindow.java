package com.arang.intake.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Computes a discovery time window (ARANG-SDD-P2 §2 / DD-P2-7 / REQ-02).
 *
 * <p>The default "this week" window is [Monday 00:00:00, Sunday 23:59:59] in the configured app
 * zone, as local wall-clock, compared against the naive {@code event.starts_at} DATETIME. DST
 * shifts are accepted: the edges are local midnight, not fixed UTC offsets.
 */
public record WeekWindow(LocalDateTime from, LocalDateTime to) {

    /** The current Mon–Sun week in the given zone. */
    public static WeekWindow thisWeek(ZoneId zone) {
        return forDate(LocalDate.now(zone));
    }

    /** The Mon–Sun week that contains {@code date}. */
    public static WeekWindow forDate(LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new WeekWindow(monday.atStartOfDay(), sunday.atTime(LocalTime.of(23, 59, 59)));
    }

    public boolean contains(LocalDateTime t) {
        return t != null && !t.isBefore(from) && !t.isAfter(to);
    }
}
