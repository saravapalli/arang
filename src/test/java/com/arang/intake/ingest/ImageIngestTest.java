package com.arang.intake.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.arang.intake.media.MediaFetcher;
import com.arang.intake.model.Event;
import com.arang.intake.model.EventMetadata;
import com.arang.intake.ocr.OcrService;
import com.arang.intake.dao.EventMetadataDao;
import com.arang.intake.dao.EventDao;
import com.arang.intake.dao.ProcessedMessageDao;
import com.arang.intake.dao.UserDao;
import com.arang.intake.service.IngestResult;
import com.arang.intake.service.IngestService;

/** T7 — image intake: media fetch + OCR + failure path (REQ-03, REQ-04). */
@SpringBootTest
@ActiveProfiles("test")
class ImageIngestTest {

    /** Override media fetcher + OCR so the success path is exercised hermetically. */
    @TestConfiguration
    static class Stubs {
        @Bean @Primary
        MediaFetcher successfulFetcher() {
            return mediaId -> (mediaId == null || mediaId.isBlank())
                ? Optional.empty()
                : Optional.of(new MediaFetcher.FetchedMedia("/tmp/arang-media/" + mediaId, new byte[]{1, 2, 3}));
        }
        @Bean @Primary
        OcrService ocrWithText() {
            return bytes -> "Jazz Night tickets https://ex.com/buy";
        }
    }

    @Autowired private IngestService ingestService;
    @Autowired private EventDao events;
    @Autowired private EventMetadataDao metadata;
    @Autowired private UserDao users;
    @Autowired private ProcessedMessageDao ledger;

    @BeforeEach
    void clean() {
        ledger.deleteAll();
        metadata.deleteAll();
        events.deleteAll();
        users.deleteAll();
    }

    @Test
    void imageSuccessStoresMediaRefAndExtractsFromOcrText() {
        IngestResult r = ingestService.ingestImage("media-1", "see flyer", "15551230000", "Mary");
        Event e = events.findById(r.summary().eventId()).orElseThrow();

        assertThat(e.getSourceType()).isEqualTo("image");                 // REQ-03
        assertThat(e.getMediaRef()).isEqualTo("/tmp/arang-media/media-1"); // REQ-03 media_ref
        // URL from OCR text extracted to metadata (REQ-05 reused)
        List<EventMetadata> md = metadata.findByEventId(e.getId());
        assertThat(md).anyMatch(m -> "https://ex.com/buy".equals(m.getValue()) && "url".equals(m.getKind()));
    }

    @Test
    void imageFetchFailureStillCreatesDraftAndMarksNotRetrieved() {
        // Default LocalMediaFetcher returns empty when no creds — but Stubs overrides with a
        // successful one. Use a blank mediaId to force empty() from the stub (Optional path):
        // instead, assert the failure path via the real default by calling with the no-cred path.
        // Simpler: the stub always succeeds, so test the failure branch directly with a null id.
        IngestResult r = ingestService.ingestImage(null, "caption only https://x.io", "15551230000", "Mary");
        Event e = events.findById(r.summary().eventId()).orElseThrow();

        assertThat(e.getSourceType()).isEqualTo("image");       // REQ-04 still source_type=image
        assertThat(e.getMediaRef()).isNull();                   // media not retrieved
        // media flagged not-retrieved
        List<EventMetadata> md = metadata.findByEventId(e.getId());
        assertThat(md).anyMatch(m -> "media".equals(m.getKey()) && "not-retrieved".equals(m.getValue()));
        // caption URL still extracted
        assertThat(md).anyMatch(m -> "https://x.io".equals(m.getValue()));
    }
}
