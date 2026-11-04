package com.arang.intake.waitlist;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.arang.intake.dao.DemandSignalDao;

/**
 * Deletes waitlist signals older than the retention period and the waitlist-only users left behind
 * (ARANG-SDD-P2-1 DD-P2.1-14, task T12; REQ-P2.1-20). Runs daily. Time comes from an injected {@link Clock}
 * so tests move time instead of waiting (TTP-D2).
 */
@Component
public class WaitlistRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(WaitlistRetentionJob.class);

    private final DemandSignalDao signals;
    private final Clock clock;
    private final int retentionMonths;

    public WaitlistRetentionJob(DemandSignalDao signals, Clock clock,
                                @Value("${arang.waitlist.retention-months:24}") int retentionMonths) {
        this.signals = signals;
        this.clock = clock;
        this.retentionMonths = retentionMonths;
    }

    /** Daily at 03:30 local time. */
    @Scheduled(cron = "${arang.waitlist.retention-cron:0 30 3 * * *}")
    public void purge() {
        run();
    }

    /** The purge, callable directly from tests. Returns the number of signals removed. */
    public int run() {
        Instant cutoff = Instant.now(clock).minus(retentionMonths * 30L, ChronoUnit.DAYS);
        int signalsRemoved = signals.deleteOlderThan(cutoff);
        int usersRemoved = signals.deleteOrphanWaitlistUsers();
        if (signalsRemoved > 0 || usersRemoved > 0) {
            log.info("Waitlist retention: removed {} signals and {} orphan users", signalsRemoved, usersRemoved);
        }
        return signalsRemoved;
    }
}
