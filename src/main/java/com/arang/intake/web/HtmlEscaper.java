package com.arang.intake.web;

/**
 * Escapes untrusted text for safe placement in the server-rendered page (ARANG-SDD-P2-1 §4.5, task T7).
 * Event titles and locations come from WhatsApp, OCR, and LLM output, so they are never trusted.
 */
public final class HtmlEscaper {

    private HtmlEscaper() {
    }

    /** Escape for HTML text or a double-quoted attribute. */
    public static String html(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&#39;");
                default -> b.append(c);
            }
        }
        return b.toString();
    }

    /**
     * Escape a value embedded inside a {@code <script type="application/json">} block. Standard JSON escaping
     * does not stop {@code </script>} or an HTML comment from closing the element, so also escape {@code <},
     * {@code >}, {@code &}, and the line separators U+2028/U+2029 as {@code \ u} sequences.
     */
    public static String jsonInScript(String json) {
        if (json == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(json.length() + 16);
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            switch (c) {
                case '<' -> b.append("\\u003c");
                case '>' -> b.append("\\u003e");
                case '&' -> b.append("\\u0026");
                case '\u2028' -> b.append("\\u2028");
                case '\u2029' -> b.append("\\u2029");
                default -> b.append(c);
            }
        }
        return b.toString();
    }
}
