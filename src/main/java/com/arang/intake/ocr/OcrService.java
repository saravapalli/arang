package com.arang.intake.ocr;

/**
 * OCR abstraction (REQ-03). Kept behind an interface so the app and tests run without native
 * Tesseract/Tess4J binaries; a real implementation can be provided behind config later (same
 * pattern as the WhatsApp client). The stub returns empty text — a draft is still created with
 * low/absent confidence, never rejected (per the design: capture losslessly, correct on review).
 *
 * <p>Assumption (recorded): P1 ships the stub OCR so the pipeline is complete and testable;
 * wiring a real Tess4J engine is a drop-in replacement of this bean and does not change the
 * event/schema/flow.
 */
public interface OcrService {

    /**
     * Extract text from image bytes.
     *
     * @return the recognized text, or empty string if nothing could be read / no engine present.
     */
    String extractText(byte[] imageBytes);
}
