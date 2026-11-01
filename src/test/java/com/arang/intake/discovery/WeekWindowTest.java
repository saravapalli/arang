package com.arang.intake.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.arang.intake.service.WeekWindow;

/** TC-03 — WeekWindow computes Mon 00:00 .. Sun 23:59:59, including a DST-crossing week. */
class WeekWindowTest {

    @Test
    void computesMondayToSundayWindow() {
        // 2026-10-30 is a Friday; its week is Mon 2026-10-26 .. Sun 2026-11-01.
        WeekWindow w = WeekWindow.forDate(LocalDate.of(2026, 10, 30));
        assertThat(w.from()).isEqualTo(LocalDateTime.of(2026, 10, 26, 0, 0, 0));
        assertThat(w.to()).isEqualTo(LocalDateTime.of(2026, 11, 1, 23, 59, 59));
    }

    @Test
    void edgesStayAtLocalMidnightAcrossDstChange() {
        // US DST ends Sun 2026-11-01. The window edges are local midnight, not fixed UTC offsets.
        WeekWindow w = WeekWindow.forDate(LocalDate.of(2026, 10, 28));
        assertThat(w.from()).isEqualTo(LocalDateTime.of(2026, 10, 26, 0, 0, 0));
        assertThat(w.to()).isEqualTo(LocalDateTime.of(2026, 11, 1, 23, 59, 59));
    }

    @Test
    void containsRespectsBothEdges() {
        WeekWindow w = WeekWindow.forDate(LocalDate.of(2026, 10, 30));
        assertThat(w.contains(LocalDateTime.of(2026, 10, 26, 0, 0, 0))).isTrue();
        assertThat(w.contains(LocalDateTime.of(2026, 11, 1, 23, 59, 59))).isTrue();
        assertThat(w.contains(LocalDateTime.of(2026, 11, 2, 0, 0, 0))).isFalse();
        assertThat(w.contains(LocalDateTime.of(2026, 10, 25, 23, 59, 59))).isFalse();
    }
}
