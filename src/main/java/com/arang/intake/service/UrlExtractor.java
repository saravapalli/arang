package com.arang.intake.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Rule-based URL extraction for P1 (REQ-05). Deliberately minimal and honest: pull out
 * http/https URLs verbatim; no typed-core (title/date/location) guessing — that is deferred to
 * the review step or a later LLM pass (per the design philosophy: capture losslessly, interpret
 * later).
 */
@Component
public class UrlExtractor {

    // http(s) URLs; trailing punctuation trimmed so "see http://x.com." yields "http://x.com".
    private static final Pattern URL = Pattern.compile(
        "https?://[^\\s<>\"]+",
        Pattern.CASE_INSENSITIVE);

    /** Returns each matched URL verbatim (trailing sentence punctuation removed), in order. */
    public List<String> extract(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return urls;
        }
        Matcher m = URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            // strip common trailing punctuation that is almost certainly not part of the URL
            while (!url.isEmpty() && ".,;:)]}>\"'".indexOf(url.charAt(url.length() - 1)) >= 0) {
                url = url.substring(0, url.length() - 1);
            }
            if (!url.isEmpty()) {
                urls.add(url);
            }
        }
        return urls;
    }
}
