package com.arang.intake.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arang.intake.town.Town;
import com.arang.intake.town.TownRegistry;

/**
 * Serves {@code /sitemap.xml} and {@code /robots.txt} (ARANG-SDD-P2-1 §4.6, task T8). The sitemap lists the landing
 * page and exactly the live-town URLs. Robots disallows the write and admin routes and names the sitemap.
 */
@RestController
public class SitemapController {

    private static final String[] DISALLOW = {
        "/add", "/submit", "/console", "/review/", "/api/", "/webhook/", "/media/", "/tiles/", "/health", "/actuator/"
    };

    private final TownRegistry towns;
    private final SiteConfigController site;

    public SitemapController(TownRegistry towns, SiteConfigController site) {
        this.towns = towns;
        this.site = site;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String base = base();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        xml.append("  <url><loc>").append(xmlText(base + "/")).append("</loc></url>\n");
        for (Town t : towns.liveTowns()) {
            xml.append("  <url><loc>").append(xmlText(base + "/towns/" + t.slug())).append("</loc></url>\n");
        }
        xml.append("</urlset>\n");
        return ResponseEntity.ok().header("Cache-Control", "public, max-age=60").body(xml.toString());
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> robots() {
        StringBuilder txt = new StringBuilder("User-agent: *\n");
        for (String path : DISALLOW) {
            txt.append("Disallow: ").append(path).append('\n');
        }
        txt.append("\nSitemap: ").append(base()).append("/sitemap.xml\n");
        return ResponseEntity.ok().body(txt.toString());
    }

    private String base() {
        String u = site.publicUrl();
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }

    private static String xmlText(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
