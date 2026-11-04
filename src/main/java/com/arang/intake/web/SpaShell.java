package com.arang.intake.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Builds a server-rendered HTML page from the built SPA's {@code index.html} (ARANG-SDD-P2-1 §4.5, DD-P2.1-3, T7):
 * it injects the page title, a canonical link, a description, optional {@code noindex}, the embedded site-config JSON,
 * and plain HTML content, then the SPA mounts over {@code #root}.
 *
 * <p>Fail-closed: if the built page lacks the {@code <title>} or the {@code <div id="root">} markers, the shell logs
 * one error and serves the page unmodified, so the SPA still loads.
 */
@Component
public class SpaShell {

    private static final Logger log = LoggerFactory.getLogger(SpaShell.class);
    private static final Pattern TITLE = Pattern.compile("<title>[^<]*</title>");
    private static final String ROOT = "<div id=\"root\"></div>";

    private final String template;
    private final boolean usable;

    public SpaShell(ResourceLoader loader) {
        String html;
        boolean ok;
        try {
            Resource r = loader.getResource("classpath:static/index.html");
            html = r.isReadable() ? new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ok = TITLE.matcher(html).find() && html.contains(ROOT);
        if (!ok) {
            log.error("SPA index.html is missing the <title> or #root marker; server pages will be unmodified");
        }
        this.template = html;
        this.usable = ok;
    }

    /**
     * Render a page. All text arguments are escaped here; callers pass raw values.
     *
     * @param title       page title (already includes the brand)
     * @param canonical   absolute canonical URL
     * @param description meta description
     * @param noindex     add {@code <meta name="robots" content="noindex">} when true
     * @param headExtra   already-safe extra head HTML, or empty
     * @param bodyContent already-escaped body HTML inserted before {@code #root} (built by the controller), or empty
     * @param siteConfigJson site-config JSON to embed (escaped for a script block here)
     */
    public String render(String title, String canonical, String description, boolean noindex,
                         String headExtra, String bodyContent, String siteConfigJson) {
        if (!usable) {
            return template;
        }
        StringBuilder head = new StringBuilder();
        head.append("<link rel=\"canonical\" href=\"").append(HtmlEscaper.html(canonical)).append("\"/>");
        head.append("<meta name=\"description\" content=\"").append(HtmlEscaper.html(description)).append("\"/>");
        if (noindex) {
            head.append("<meta name=\"robots\" content=\"noindex\"/>");
        }
        head.append("<script id=\"site-config\" type=\"application/json\">")
            .append(HtmlEscaper.jsonInScript(siteConfigJson))
            .append("</script>");
        if (headExtra != null && !headExtra.isEmpty()) {
            head.append(headExtra);
        }

        String out = TITLE.matcher(template)
            .replaceFirst(Matcher.quoteReplacement("<title>" + HtmlEscaper.html(title) + "</title>" + head));
        // Put the crawler content INSIDE #root. React's createRoot().render() clears #root's children when it
        // mounts, so a browser ends with one clean styled app; a crawler (no scripts) still sees the content.
        String rootWithContent = "<div id=\"root\">" + (bodyContent == null ? "" : bodyContent) + "</div>";
        out = out.replace(ROOT, Matcher.quoteReplacement(rootWithContent));
        return out;
    }
}
