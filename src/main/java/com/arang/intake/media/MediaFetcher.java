package com.arang.intake.media;

import java.util.Optional;

/**
 * Fetches inbound WhatsApp media to local storage (REQ-03, DD-5 local filesystem). Returns the
 * stored reference (a local path) and the bytes, or empty if the download fails (REQ-04 handles
 * the failure path: draft still created from caption, media marked not-retrieved).
 */
public interface MediaFetcher {

    /**
     * @param mediaId the Cloud API media id from the inbound image message
     * @return the fetched media, or empty if it could not be retrieved
     */
    Optional<FetchedMedia> fetch(String mediaId);

    /** A successfully fetched+stored media file. */
    record FetchedMedia(String mediaRef, byte[] bytes) {
    }
}
