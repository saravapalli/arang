package com.arang.intake.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Persists uploaded image bytes (e.g. a console flyer upload) to a local media directory and
 * returns a {@code media_ref} path. Fixes the P1 gap where console uploads were processed by the
 * LLM then discarded (media_ref left null). Files are served at {@code /media/**} (see
 * {@code WebConfig}). The directory is configurable via {@code arang.media.dir}.
 */
@Component
public class MediaStore {

    private static final Logger log = LoggerFactory.getLogger(MediaStore.class);

    private final Path dir;

    public MediaStore(@Value("${arang.media.dir:#{null}}") String configuredDir) {
        this.dir = (configuredDir != null && !configuredDir.isBlank())
            ? Path.of(configuredDir)
            : Path.of(System.getProperty("java.io.tmpdir"), "arang-media");
    }

    /**
     * Store the bytes under a generated filename, returning a {@code media_ref} of the form
     * {@code media/<file>} (the path the SPA/browser can GET). Returns null on failure (best-effort
     * — the draft still exists without the image).
     */
    public String store(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            Files.createDirectories(dir);
            String ext = extensionFor(mimeType);
            String name = UUID.randomUUID() + ext;
            Files.write(dir.resolve(name), bytes);
            log.info("Stored uploaded media → {}", name);
            return "media/" + name;
        } catch (IOException e) {
            log.warn("Failed to store uploaded media: {}", e.toString());
            return null;
        }
    }

    public Path directory() {
        return dir;
    }

    private static String extensionFor(String mimeType) {
        if (mimeType == null) return ".bin";
        return switch (mimeType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpeg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".bin";
        };
    }
}
