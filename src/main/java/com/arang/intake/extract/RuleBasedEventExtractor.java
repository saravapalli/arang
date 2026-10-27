package com.arang.intake.extract;

/**
 * Default extractor: no typed-core NLU. Returns {@link ExtractedFields#empty()} — honest P1
 * behavior (URLs are extracted separately by {@code UrlExtractor}; title/date/location are left
 * for the producer to fill on the review page). Used unless the LLM extractor is enabled.
 */
public class RuleBasedEventExtractor implements EventExtractor {

    @Override
    public ExtractedFields extract(String message) {
        return ExtractedFields.empty();
    }
}
