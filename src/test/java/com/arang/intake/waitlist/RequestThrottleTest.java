package com.arang.intake.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

/**
 * TC-P2.1-043, TC-P2.1-045, TC-P2.1-047 (ARANG-TTP-P2-1, task T11): per-client limit, window reset, global ceiling,
 * and separate buckets per key. Uses a movable clock (TTP-D2); no sleeping.
 */
class RequestThrottleTest {

    private static final class MovableClock extends Clock {
        final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-04T00:00:00Z"));
        void advanceSeconds(long s) { now.updateAndGet(i -> i.plusSeconds(s)); }
        @Override public Instant instant() { return now.get(); }
        @Override public long millis() { return now.get().toEpochMilli(); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
    }

    @Test
    void perClientLimitThenWindowReset() {
        MovableClock clock = new MovableClock();
        RequestThrottle t = new RequestThrottle(clock, 5, 100);
        for (int i = 0; i < 5; i++) {
            assertThat(t.allow("client-a")).as("request " + i).isTrue();
        }
        assertThat(t.allow("client-a")).as("sixth is blocked").isFalse();
        // A different client is unaffected.
        assertThat(t.allow("client-b")).isTrue();
        // After the window, client-a is allowed again.
        clock.advanceSeconds(61);
        assertThat(t.allow("client-a")).isTrue();
    }

    @Test
    void globalCeilingAcrossManyClients() {
        MovableClock clock = new MovableClock();
        RequestThrottle t = new RequestThrottle(clock, 5, 100);
        int allowed = 0;
        for (int i = 0; i < 100; i++) {           // 100 distinct clients, 2 each = 200 attempts
            if (t.allow("c" + i)) allowed++;
            if (t.allow("c" + i)) allowed++;
        }
        assertThat(allowed).isEqualTo(100);        // global ceiling caps the minute at 100
    }
}
