package com.arang.intake.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arang.intake.geo.GeoPlaceSource;
import com.arang.intake.town.Town;
import com.arang.intake.town.TownRegistry;

/**
 * Site configuration the SPA needs to render without extra calls (ARANG-SDD-P2-1 §4.1, task T5): the live towns,
 * the near-me limit, the consent template, the data credits, the geo status, and the public URL. The page shell
 * (T7) also embeds this JSON so the landing works even when place data is down.
 */
@RestController
public class SiteConfigController {

    private final TownRegistry towns;
    private final GeoPlaceSource geo;

    @Value("${arang.site.public-url:http://localhost:8080}")
    private String publicUrl;

    @Value("${arang.site.near-me-miles:100}")
    private int nearMeMiles;

    @Value("${arang.waitlist.consent-version:v1}")
    private String consentVersion;

    @Value("${arang.waitlist.consent-text:We'll only email you about Arangly in <town>.}")
    private String consentText;

    public SiteConfigController(TownRegistry towns, GeoPlaceSource geo) {
        this.towns = towns;
        this.geo = geo;
    }

    @GetMapping("/api/site-config")
    public Map<String, Object> siteConfig() {
        List<Map<String, Object>> townList = towns.liveTowns().stream().map(SiteConfigController::townJson).toList();

        Map<String, Object> consent = new LinkedHashMap<>();
        consent.put("version", consentVersion);
        consent.put("text", consentText);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("towns", townList);
        body.put("nearMeMiles", nearMeMiles);
        body.put("consent", consent);
        body.put("credits", geo.credits());
        body.put("geo", geo.status().name());
        body.put("publicUrl", publicUrl);
        return body;
    }

    private static Map<String, Object> townJson(Town t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("slug", t.slug());
        m.put("name", t.name());
        m.put("state", t.state());
        m.put("postal", t.postalCode());
        m.put("lat", t.latitude());
        m.put("lng", t.longitude());
        m.put("zoom", t.mapZoom());
        m.put("popularRank", t.popularRank());
        return m;
    }
}
