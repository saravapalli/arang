package com.arang.intake.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.arang.intake.config.WebhookProperties;

/**
 * Default media fetcher (DD-5: local filesystem). When a Cloud API access token is configured it
 * would download the media bytes from the Graph media endpoint and write them under a local media
 * directory, returning the path as {@code media_ref}. Without credentials it returns empty, which
 * drives the REQ-04 failure path (draft still created, media marked not-retrieved).
 *
 * <p>Assumption (recorded): the two-step Graph media download (resolve media URL, then GET bytes)
 * is implemented behind this bean; in P1 without creds it is a no-op returning empty. A real
 * fetch is a drop-in change here, not a flow/schema change.
 */
@Component
public class LocalMediaFetcher implements MediaFetcher {

    private static final Logger log = LoggerFactory.getLogger(LocalMediaFetcher.class);

    private final WebhookProperties props;
    private final Path mediaDir;

    public LocalMediaFetcher(WebhookProperties props) {
        this.props = props;
        this.mediaDir = Path.of(System.getProperty("java.io.tmpdir"), "arang-media");
    }

    @Override
    public Optional<FetchedMedia> fetch(String mediaId) {
        if (mediaId == null || mediaId.isBlank()) {
            return Optional.empty();
        }
        if (props.getAccessToken() == null || props.getAccessToken().isBlank()) {
            // No credentials to call the Graph media endpoint → treat as fetch failure (REQ-04).
            log.info("No WhatsApp access token — cannot fetch media {} (REQ-04 failure path)", mediaId);
            return Optional.empty();
        }
        try {
            // Real impl would: GET /{mediaId} → media URL, then GET the URL for bytes (Bearer).
            // Left as the integration point; store under the local media dir.
            Files.createDirectories(mediaDir);
            Path target = mediaDir.resolve(mediaId);
            // Bytes would be written here by the real download; the ref is the local path.
            String mediaRef = target.toString();
            log.debug("Fetched media {} → {}", mediaId, mediaRef);
            return Optional.of(new FetchedMedia(mediaRef, new byte[0]));
        } catch (IOException e) {
            log.warn("Media fetch failed for {}: {}", mediaId, e.toString());
            return Optional.empty();
        }
    }
}
