package com.arang.intake.ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default no-op OCR: returns empty text. The only OCR impl in P1 (a real Tesseract/Tess4J engine
 * is a later drop-in replacement). Lets the image pipeline run end-to-end (fetch → store → "OCR"
 * → draft) without native deps.
 */
@Component
public class StubOcrService implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(StubOcrService.class);

    @Override
    public String extractText(byte[] imageBytes) {
        log.info("[stub OCR] no OCR engine configured — returning empty text ({} bytes ignored)",
            imageBytes == null ? 0 : imageBytes.length);
        return "";
    }
}
