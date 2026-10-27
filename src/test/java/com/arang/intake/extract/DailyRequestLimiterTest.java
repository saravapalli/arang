package com.arang.intake.extract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The hard daily cap: allows exactly the limit, then blocks. */
class DailyRequestLimiterTest {

    @Test
    void allowsUpToLimitThenBlocks() {
        DailyRequestLimiter limiter = new DailyRequestLimiter(3);
        assertThat(limiter.tryAcquire()).isTrue();   // 1
        assertThat(limiter.tryAcquire()).isTrue();   // 2
        assertThat(limiter.tryAcquire()).isTrue();   // 3
        assertThat(limiter.tryAcquire()).isFalse();  // 4 → blocked
        assertThat(limiter.tryAcquire()).isFalse();  // stays blocked
    }

    @Test
    void remainingCountsDown() {
        DailyRequestLimiter limiter = new DailyRequestLimiter(70);
        assertThat(limiter.remainingToday()).isEqualTo(70);
        limiter.tryAcquire();
        limiter.tryAcquire();
        assertThat(limiter.remainingToday()).isEqualTo(68);
    }

    @Test
    void zeroLimitBlocksImmediately() {
        DailyRequestLimiter limiter = new DailyRequestLimiter(0);
        assertThat(limiter.tryAcquire()).isFalse();
        assertThat(limiter.remainingToday()).isZero();
    }
}
