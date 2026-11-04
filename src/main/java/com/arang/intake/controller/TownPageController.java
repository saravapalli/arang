package com.arang.intake.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.arang.intake.dto.DiscoveryResult;
import com.arang.intake.geo.GeoPlaceSource;
import com.arang.intake.geo.Place;
import com.arang.intake.service.DiscoveryService;
import com.arang.intake.service.WeekWindow;
import com.arang.intake.town.Town;
import com.arang.intake.town.TownRegistry;
import com.arang.intake.web.HtmlEscaper;
import com.arang.intake.web.SpaShell;
import tools.jackson.databind.ObjectMapper;

/**
 * Serves the landing ({@code /}) and per-town pages ({@code /towns/{slug}}) as server-rendered HTML so a crawler
 * that does not run scripts sees the town and its events (ARANG-SDD-P2-1 §4.5, DD-P2.1-3/5, task T7). The SPA mounts
 * over {@code #root} and takes over.
 *
 * <p>Decision table for {@code /towns/{slug}}: uppercase or trailing slash → 301 to the lowercase form; live town →
 * 200; alias of a live town → 301 to the database slug; resolvable not-live place → 200 coming-soon + noindex; alias
 * of a hidden town → the hidden town's behavior; unknown/malformed, or geo down for a non-live slug → 404 landing.
 */
@RestController
public class TownPageController {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final int MAX_EVENTS_ON_PAGE = 30;

    private final TownRegistry towns;
    private final GeoPlaceSource geo;
    private final DiscoveryService discovery;
    private final SiteConfigController siteConfig;
    private final SpaShell shell;
    private final ObjectMapper json;

    @Value("${arang.site.public-url:http://localhost:8080}")
    private String publicUrl;

    @Value("${arang.discovery.timezone:America/New_York}")
    private String zoneId;

    @Value("${arang.site.page-cache-seconds:60}")
    private long cacheSeconds;

    public TownPageController(TownRegistry towns, GeoPlaceSource geo, DiscoveryService discovery,
                              SiteConfigController siteConfig, SpaShell shell, ObjectMapper json) {
        this.towns = towns;
        this.geo = geo;
        this.discovery = discovery;
        this.siteConfig = siteConfig;
        this.shell = shell;
        this.json = json;
    }

    @GetMapping(value = {"/", ""}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> landing() {
        String body = "<main><h1>What\u2019s on in your town?</h1>"
            + "<p>Arangly is for your own town. Choose a town to see local events.</p><ul>"
            + towns.liveTowns().stream()
                .map(t -> "<li><a href=\"/towns/" + t.slug() + "\">" + HtmlEscaper.html(t.name() + ", " + t.state())
                    + "</a></li>")
                .reduce("", String::concat)
            + "</ul></main>";
        String html = shell.render("Arangly — events in your town", canonical("/"),
            "Find local events in your Massachusetts town on Arangly.", false, "", body, siteConfigJson());
        return htmlResponse(html, true);
    }

    @GetMapping(value = {"/towns/{slug}", "/towns/{slug}/"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> townPage(@PathVariable String slug, jakarta.servlet.http.HttpServletRequest req) {
        String uri = req.getRequestURI();
        // Case or trailing-slash normalization → one permanent redirect to the clean lowercase form.
        String afterPrefix = uri.substring("/towns/".length());
        String normalized = afterPrefix.toLowerCase();
        if (normalized.endsWith("/")) {
            normalized = normalized.replaceAll("/+$", "");
        }
        if (!afterPrefix.equals(normalized)) {
            return redirect("/towns/" + normalized);
        }
        if (!SLUG.matcher(slug).matches()) {
            return landingNotFound();
        }

        Optional<Town> live = towns.bySlug(slug).filter(Town::isLive);
        if (live.isPresent()) {
            return liveTownPage(live.get());
        }
        // Alias → redirect to the current slug (built from the database, never the request path).
        Optional<String> aliasTarget = towns.resolveAlias(slug);
        if (aliasTarget.isPresent() && !aliasTarget.get().equals(slug)) {
            return redirect("/towns/" + aliasTarget.get());
        }
        // Resolvable but not live → coming soon (needs the place file).
        if (geo.isUp()) {
            Optional<Place> place = geo.bySlug(slug);
            if (place.isPresent()) {
                return comingSoonPage(place.get());
            }
        }
        return landingNotFound();
    }

    private ResponseEntity<String> liveTownPage(Town town) {
        ZoneId zone = ZoneId.of(zoneId);
        WeekWindow week = WeekWindow.thisWeek(zone);
        List<DiscoveryResult> events;
        try {
            events = discovery.find(week.from(), week.to(), town.postalCode(), List.of());
        } catch (RuntimeException e) {
            events = List.of();
        }
        StringBuilder body = new StringBuilder("<main><h1>")
            .append(HtmlEscaper.html("What\u2019s on in " + town.name() + ", " + town.state()))
            .append("</h1>");
        if (events.isEmpty()) {
            body.append("<p>No events listed this week.</p>");
        } else {
            body.append("<ul>");
            events.stream().limit(MAX_EVENTS_ON_PAGE).forEach(e -> body.append("<li>")
                .append(HtmlEscaper.html(e.title()))
                .append(e.startsAt() == null ? "" : " — " + HtmlEscaper.html(e.startsAt()))
                .append(e.location() == null ? "" : " — " + HtmlEscaper.html(e.location()))
                .append("</li>"));
            body.append("</ul>");
        }
        body.append("</main>");
        String title = town.name() + ", " + town.state() + " events — Arangly";
        String desc = "Local events in " + town.name() + ", " + town.state() + " this week on Arangly.";
        String html = shell.render(title, canonical("/towns/" + town.slug()), desc, false, "",
            body.toString(), siteConfigJson());
        return htmlResponse(html, true);
    }

    private ResponseEntity<String> comingSoonPage(Place place) {
        String label = place.name() + ", " + place.state();
        String body = "<main><h1>" + HtmlEscaper.html("Coming soon to " + label) + "</h1>"
            + "<p>" + HtmlEscaper.html("Arangly is not live in " + label + " yet. Leave your email to hear when it is.")
            + "</p></main>";
        String title = "Coming soon to " + label + " — Arangly";
        String desc = "Arangly is coming to " + label + ". Leave your email to be notified.";
        String html = shell.render(title, canonical("/towns/" + place.slug()), desc, true, "", body,
            siteConfigJson());
        return htmlResponse(html, false);
    }

    private ResponseEntity<String> landingNotFound() {
        // Visitors see the landing; crawlers get a real 404 so bad URLs are dropped (DD-P2.1-5).
        String html = shell.render("Arangly — events in your town", canonical("/"),
            "Find local events in your Massachusetts town on Arangly.", false, "",
            "<main><h1>What\u2019s on in your town?</h1><p>Choose a town to see local events.</p></main>",
            siteConfigJson());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.TEXT_HTML).body(html);
    }

    private ResponseEntity<String> redirect(String location) {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
            .header("Location", location).build();
    }

    private ResponseEntity<String> htmlResponse(String html, boolean cache) {
        ResponseEntity.BodyBuilder b = ResponseEntity.ok().contentType(MediaType.TEXT_HTML);
        if (cache) {
            b.header("Cache-Control", "public, max-age=" + cacheSeconds);
        }
        return b.body(html);
    }

    private String canonical(String path) {
        String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        return base + path;
    }

    private String siteConfigJson() {
        try {
            return json.writeValueAsString(siteConfig.config());
        } catch (Exception e) {
            return "{}";
        }
    }
}
