package com.arang.intake.waitlist;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-memory rate limit for waitlist posts (ARANG-SDD-P2-1 DD-P2.1-7, task T11): a per-client limit and a global
 * ceiling, both over a one-minute sliding window. The per-client map is bounded and evicts idle entries, so it
 * cannot grow without bound. Per instance only; a shared store is P4 hardening.
 *
 * <p>Time comes from an injected {@link Clock} so tests move time instead of sleeping (TTP-D2).
 */
@Component
public class RequestThrottle {

    private static final long WINDOW_MS = 60_000;
    private static final int MAX_CLIENTS = 10_000;

    private final Clock clock;
    private final int perClientPerMinute;
    private final int globalPerMinute;

    /** Bounded LRU map of client key → recent request timestamps (ms). */
    private final Map<String, Deque<Long>> perClient = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Deque<Long>> eldest) {
            return size() > MAX_CLIENTS;
        }
    };
    private final Deque<Long> global = new ArrayDeque<>();

    public RequestThrottle(Clock clock,
                           @Value("${arang.waitlist.rate-per-minute:5}") int perClientPerMinute,
                           @Value("${arang.waitlist.global-rate-per-minute:100}") int globalPerMinute) {
        this.clock = clock;
        this.perClientPerMinute = perClientPerMinute;
        this.globalPerMinute = globalPerMinute;
    }

    /** Record an attempt for the client key; return true if it is allowed, false if a limit is exceeded. */
    public synchronized boolean allow(String clientKey) {
        long now = clock.millis();
        long cutoff = now - WINDOW_MS;

        prune(global, cutoff);
        if (global.size() >= globalPerMinute) {
            return false;
        }
        Deque<Long> times = perClient.computeIfAbsent(clientKey, k -> new ArrayDeque<>());
        prune(times, cutoff);
        if (times.size() >= perClientPerMinute) {
            return false;
        }
        times.addLast(now);
        global.addLast(now);
        return true;
    }

    private static void prune(Deque<Long> q, long cutoff) {
        while (!q.isEmpty() && q.peekFirst() < cutoff) {
            q.pollFirst();
        }
    }
}
