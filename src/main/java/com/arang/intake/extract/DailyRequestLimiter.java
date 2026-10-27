package com.arang.intake.extract;

import java.time.LocalDate;

/**
 * Hard cap on requests per local calendar day — protects a trial API quota (e.g. Gemini free
 * tier). Thread-safe; the counter resets automatically when the local date rolls over. Not
 * persisted: a process restart resets the count (acceptable for the prototype; a DB-backed
 * counter would be the hardening step for multi-instance / restart-proof limits).
 */
public class DailyRequestLimiter {

    private final int dailyLimit;
    private LocalDate day = LocalDate.now();
    private int used = 0;

    public DailyRequestLimiter(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    /**
     * Try to consume one request for today. Rolls the counter over at local midnight.
     *
     * @return true if allowed (and counted); false if today's cap is already reached.
     */
    public synchronized boolean tryAcquire() {
        LocalDate today = LocalDate.now();
        if (!today.equals(day)) {   // new day → reset
            day = today;
            used = 0;
        }
        if (used >= dailyLimit) {
            return false;
        }
        used++;
        return true;
    }

    public synchronized int remainingToday() {
        LocalDate today = LocalDate.now();
        if (!today.equals(day)) {
            return dailyLimit;
        }
        return Math.max(0, dailyLimit - used);
    }

    public int getDailyLimit() {
        return dailyLimit;
    }
}
