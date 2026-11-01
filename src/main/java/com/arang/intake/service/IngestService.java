package com.arang.intake.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.arang.intake.dao.EventDao;
import com.arang.intake.dao.EventMetadataDao;
import com.arang.intake.dao.ProcessedMessageDao;
import com.arang.intake.dao.UserDao;
import com.arang.intake.media.MediaFetcher;
import com.arang.intake.model.Event;
import com.arang.intake.model.EventMetadata;
import com.arang.intake.model.ProcessedMessage;
import com.arang.intake.model.User;
import com.arang.intake.ocr.OcrService;
import com.arang.intake.reply.DraftSummary;

/**
 * Turns an inbound WhatsApp message (text or image) into a draft event.
 *
 * <p>Text (T3): REQ-02/05/06/15. Image (T7): REQ-03 (fetch media, OCR, store text, source_type
 * =image, media_ref) and REQ-04 (on media-fetch failure still create a draft, mark media
 * not-retrieved, ask for a resend). URL extraction and the confidence roll-up are shared.
 */
@Service
public class IngestService {

    /** A URL match via regex is certain. */
    private static final BigDecimal URL_CONFIDENCE = new BigDecimal("1.00");

    private final UserDao users;
    private final EventDao events;
    private final EventMetadataDao metadata;
    private final ProcessedMessageDao ledger;
    private final UrlExtractor urlExtractor;
    private final EditTokenService editTokens;
    private final MediaFetcher mediaFetcher;
    private final OcrService ocrService;
    private final EventEnrichmentService enrichment;
    private final com.arang.intake.media.MediaStore mediaStore;

    public IngestService(UserDao users, EventDao events,
                         EventMetadataDao metadata, ProcessedMessageDao ledger,
                         UrlExtractor urlExtractor, EditTokenService editTokens,
                         MediaFetcher mediaFetcher, OcrService ocrService,
                         EventEnrichmentService enrichment,
                         com.arang.intake.media.MediaStore mediaStore) {
        this.users = users;
        this.events = events;
        this.metadata = metadata;
        this.ledger = ledger;
        this.urlExtractor = urlExtractor;
        this.editTokens = editTokens;
        this.mediaFetcher = mediaFetcher;
        this.ocrService = ocrService;
        this.enrichment = enrichment;
        this.mediaStore = mediaStore;
    }

    /**
     * Transactional per-message unit (REQ-07 + ingest). Deduplicates on the provider message id;
     * records it in the ledger; dispatches text → {@link #ingestText} and image → {@link
     * #ingestImage}; links the created event to the ledger row. Returns the ingest result (with a
     * recipient + reply summary), or null for a duplicate or an unsupported type. The reply is
     * sent by the caller after this commits (REQ-08 send-after-commit).
     */
    @Transactional
    public IngestResult processMessage(String providerMessageId, String type, String text,
                                       String mediaId, String senderPhone, String senderName) {
        if (ledger.existsById(providerMessageId)) {
            return null; // REQ-07 idempotency
        }
        ledger.insert(new ProcessedMessage(providerMessageId));

        IngestResult result;
        if ("text".equals(type)) {
            result = ingestText(text, senderPhone, senderName);
        } else if ("image".equals(type)) {
            result = ingestImage(mediaId, text, senderPhone, senderName);
        } else {
            return null; // other types recorded for idempotency only
        }
        ledger.linkEvent(providerMessageId, result.summary().eventId());
        return result;
    }

    /** Kept for direct use/tests (T3). Delegates to the shared draft builder. */
    @Transactional
    public IngestResult ingestText(String text, String senderPhone, String senderName) {
        Long submittedBy = resolveUserId(senderPhone, senderName);
        Event saved = createDraft("text", text, null, submittedBy);
        List<String> urls = extractAndStoreUrls(saved, text);
        finishConfidence(saved, urls);
        enrichment.enrichAsync(saved.getId(), text);   // best-effort LLM field extraction
        return new IngestResult(senderPhone, summaryOf(saved, urls, false));
    }

    /**
     * Image ingestion (REQ-03/04). Fetches the media to local storage; on success OCRs it and
     * extracts URLs from the OCR text, {@code source_type=image}, {@code media_ref} set. On fetch
     * failure still creates the draft from any caption, marks media not-retrieved, and flags the
     * reply to ask for a resend.
     */
    @Transactional
    public IngestResult ingestImage(String mediaId, String caption, String senderPhone, String senderName) {
        Long submittedBy = resolveUserId(senderPhone, senderName);
        Optional<MediaFetcher.FetchedMedia> fetched = mediaFetcher.fetch(mediaId);

        if (fetched.isEmpty()) {
            // REQ-04: media not retrieved → draft from caption, mark not-retrieved, ask resend.
            Event saved = createDraft("image", caption, null, submittedBy);
            metadata.insert(new EventMetadata(saved.getId(), "media", "not-retrieved", "text", null, "system"));
            List<String> urls = extractAndStoreUrls(saved, caption);
            finishConfidence(saved, urls);
            enrichment.enrichAsync(saved.getId(), caption);
            return new IngestResult(senderPhone, summaryOf(saved, urls, true)); // resend flag
        }

        // REQ-03: media stored; OCR → text; extract URLs from OCR text + caption.
        MediaFetcher.FetchedMedia media = fetched.get();
        String ocrText = ocrService.extractText(media.bytes());
        String combined = joinNonBlank(caption, ocrText);
        Event saved = createDraft("image", combined, media.mediaRef(), submittedBy);
        List<String> urls = extractAndStoreUrls(saved, combined);
        finishConfidence(saved, urls);
        enrichment.enrichAsync(saved.getId(), combined);
        return new IngestResult(senderPhone, summaryOf(saved, urls, false));
    }

    // ---- shared helpers ----

    /**
     * Console image upload (flyer): create an image draft from already-fetched local bytes, then
     * run multimodal LLM enrichment asynchronously (Gemini reads the flyer). The caption, if any,
     * is stored as raw_message and its URLs extracted. Returns the created draft so the controller
     * can return the review link; enrichment lands shortly after via the status poll.
     */
    @Transactional
    public Event ingestImageBytes(byte[] imageBytes, String mimeType, String caption,
                                  String senderPhone, String senderName) {
        Long submittedBy = resolveUserId(senderPhone, senderName);
        String mediaRef = mediaStore.store(imageBytes, mimeType); // persist the flyer (P1 fix)
        Event saved = createDraft("image", caption, mediaRef, submittedBy);
        List<String> urls = extractAndStoreUrls(saved, caption);
        finishConfidence(saved, urls);
        enrichment.enrichFromImageAsync(saved.getId(), imageBytes, mimeType, caption);
        return saved;
    }

    /**
     * Direct-entry web form (REQ-14): create a draft of the same shape as a WhatsApp submission,
     * {@code origin_type=user}, no WhatsApp id (no phone → {@code submitted_by} null). Returns the
     * created event so the controller can redirect to the token-gated review page.
     */
    @Transactional
    public Event ingestForm(String title, String location, String postalCode, String freeText) {
        String raw = joinNonBlank(title, joinNonBlank(location, freeText));
        Event saved = createDraft("text", raw, null, null); // origin_type=user set in createDraft
        if (title != null && !title.isBlank()) {
            saved.setTitle(title.trim());
        }
        if (location != null && !location.isBlank()) {
            saved.setLocation(location.trim());
        }
        if (postalCode != null && !postalCode.isBlank()) {
            saved.setPostalCode(postalCode.trim());
        }
        List<String> urls = extractAndStoreUrls(saved, freeText);
        finishConfidence(saved, urls);
        return saved;
    }

    private Event createDraft(String sourceType, String rawMessage, String mediaRef, Long submittedBy) {
        Event event = new Event();
        event.setStatus("draft");                 // REQ-02
        event.setSourceType(sourceType);
        event.setRawMessage(rawMessage);
        event.setMediaRef(mediaRef);
        event.setOriginType("user");              // REQ-15
        event.setSubmittedBy(submittedBy);        // REQ-15
        event.setEditToken(editTokens.newToken());
        event.setEditTokenExpiresAt(editTokens.expiryFromNow());
        return events.insert(event);
    }

    private List<String> extractAndStoreUrls(Event saved, String text) {
        List<String> urls = urlExtractor.extract(text); // REQ-05
        for (String url : urls) {
            metadata.insert(new EventMetadata(saved.getId(), url, url, "url", URL_CONFIDENCE, "text"));
        }
        return urls;
    }

    private void finishConfidence(Event saved, List<String> urls) {
        saved.setExtractedConfidence(urls.isEmpty() ? null : URL_CONFIDENCE); // REQ-06
        events.update(saved);
    }

    private DraftSummary summaryOf(Event saved, List<String> urls, boolean askResend) {
        // askResend is surfaced via a metadata flag already; the reply composer reads the summary.
        return new DraftSummary(
            saved.getId(),
            saved.getEditToken(),
            saved.getTitle(),
            null,
            null,
            saved.getLocation(),
            urls
        );
    }

    private static String joinNonBlank(String a, String b) {
        boolean ab = a != null && !a.isBlank();
        boolean bb = b != null && !b.isBlank();
        if (ab && bb) {
            return a + "\n" + b;
        }
        if (ab) {
            return a;
        }
        return bb ? b : null;
    }

    private Long resolveUserId(String senderPhone, String senderName) {
        if (senderPhone == null || senderPhone.isBlank()) {
            return null; // tolerate missing phone (Q4): still create the event, submitted_by null
        }
        User user = users.findByPhoneNumber(senderPhone)
            .orElseGet(() -> users.insert(new User(senderPhone, senderName)));
        return user.getId();
    }
}
