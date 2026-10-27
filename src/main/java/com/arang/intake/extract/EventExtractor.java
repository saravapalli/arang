package com.arang.intake.extract;

/**
 * Deciphers structured event fields from a free-text message. The pluggable seam the design
 * reserved for an LLM ("capture losslessly, interpret later"). Implementations:
 * <ul>
 *   <li>{@code RuleBasedEventExtractor} — default; no typed-core guessing (regex handles URLs
 *       elsewhere). Always available, fast, offline.</li>
 *   <li>{@code LlmEventExtractor} — calls a local OpenAI-compatible model; active when
 *       {@code arang.llm.enabled=true}.</li>
 * </ul>
 * Extraction is best-effort and must never be required for a draft to exist.
 */
public interface EventExtractor {

    /** @return structured fields (any may be null); never throws — returns empty on failure. */
    ExtractedFields extract(String message);

    /**
     * Multimodal extraction from an image (a flyer/poster), optionally with a caption. Default is
     * no-op (empty) — only providers that support vision (Gemini) override this. Never throws.
     *
     * @param imageBytes the raw image bytes
     * @param mimeType   e.g. {@code image/png}, {@code image/jpeg}
     * @param caption    any accompanying text (may be null)
     */
    default ExtractedFields extractFromImage(byte[] imageBytes, String mimeType, String caption) {
        return ExtractedFields.empty();
    }

    /** Whether this extractor can read images (vision-capable). */
    default boolean supportsImages() {
        return false;
    }
}
