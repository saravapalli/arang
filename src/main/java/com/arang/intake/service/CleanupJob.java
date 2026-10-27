package com.arang.intake.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.arang.intake.dao.EventDao;
import com.arang.intake.dao.EventMetadataDao;
import com.arang.intake.model.Event;

/**
 * Scheduled cleanup of abandoned drafts (SDD T9 — REQ-18): delete draft events (and their
 * metadata) not submitted within 2 months of creation. Published events are never touched.
 *
 * <p>The schedule just calls {@link #deleteStaleDrafts(Instant)}, which is public and takes the
 * cutoff as a parameter so it can be unit-tested deterministically without waiting on timing.
 */
@Service
public class CleanupJob {

    private static final Logger log = LoggerFactory.getLogger(CleanupJob.class);
    private static final long DRAFT_TTL_MONTHS = 2;

    private final EventDao events;
    private final EventMetadataDao metadata;

    public CleanupJob(EventDao events, EventMetadataDao metadata) {
        this.events = events;
        this.metadata = metadata;
    }

    /** Runs daily at 03:00. Deletes drafts older than the TTL as of "now". */
    @Scheduled(cron = "0 0 3 * * *")
    public void runScheduled() {
        Instant cutoff = Instant.now().minus(DRAFT_TTL_MONTHS * 30, ChronoUnit.DAYS);
        int deleted = deleteStaleDrafts(cutoff);
        if (deleted > 0) {
            log.info("Draft cleanup removed {} stale draft(s) older than {}", deleted, cutoff);
        }
    }

    /**
     * Delete all draft events created before {@code cutoff}, along with their metadata. Published
     * events are excluded by the query. Returns the number of events deleted.
     */
    @Transactional
    public int deleteStaleDrafts(Instant cutoff) {
        List<Event> stale = events.findStaleDrafts(cutoff);
        for (Event e : stale) {
            metadata.deleteByEventId(e.getId());
            events.deleteById(e.getId());
        }
        return stale.size();
    }
}
