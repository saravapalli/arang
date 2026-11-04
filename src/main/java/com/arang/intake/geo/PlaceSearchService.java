package com.arang.intake.geo;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.arang.intake.town.TownRegistry;

/**
 * Town-name search over the in-memory place list (ARANG-SDD-P2-1 §3.4, task T6). P2.1 searches by name only;
 * ZIP search is P2.3. Matching runs in Java, so no query is built from visitor text (no injection surface).
 */
@Service
public class PlaceSearchService {

    /** Max suggestions returned besides exact matches (ARANG-SDD-P2-1 §3.4). */
    public static final int MAX_RESULTS = 8;

    private static final Pattern DIGITS_ONLY = Pattern.compile("[0-9 \\-]+");
    private static final Pattern PUNCT = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> STATE_WORDS = Set.of("ma", "mass", "massachusetts");

    private final GeoPlaceSource places;
    private final TownRegistry towns;

    public PlaceSearchService(GeoPlaceSource places, TownRegistry towns) {
        this.places = places;
        this.towns = towns;
    }

    /** One search hit. {@code live} = Arangly serves it now; {@code exact} = the whole name equals the query. */
    public record Hit(String slug, String name, String state, boolean live, boolean exact) {}

    /** The outcome of a search: either results, or a hint that the input was digits only (type a town name). */
    public record SearchResult(List<Hit> results, boolean digitsHint) {
        static SearchResult of(List<Hit> hits) {
            return new SearchResult(hits, false);
        }

        static SearchResult digits() {
            return new SearchResult(List.of(), true);
        }
    }

    public boolean isUp() {
        return places.isUp();
    }

    /** Normalize search text the same way the build script does: the two must agree (build check in T2). */
    static String normalize(String text) {
        String t = Normalizer.normalize(text, Normalizer.Form.NFKD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase();
        t = PUNCT.matcher(t).replaceAll(" ").trim();
        if (t.isEmpty()) {
            return "";
        }
        String[] words = t.split(" ");
        int end = words.length;
        if (end > 1 && STATE_WORDS.contains(words[end - 1])) {
            end--;
        }
        return String.join(" ", List.of(words).subList(0, end));
    }

    /** Word-start rule: every typed word starts some word of the name, in any order. */
    static boolean matches(String normalizedQuery, String searchName) {
        if (normalizedQuery.isEmpty()) {
            return false;
        }
        String[] nameWords = searchName.split(" ");
        for (String qw : normalizedQuery.split(" ")) {
            boolean hit = false;
            for (String nw : nameWords) {
                if (nw.startsWith(qw)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                return false;
            }
        }
        return true;
    }

    /**
     * Search for the raw text. Digits-only input (for example a ZIP) returns {@code digitsHint=true} and no results,
     * so the UI shows "type a town name" rather than "not found" (ZIP search is P2.3).
     */
    public SearchResult search(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (!trimmed.isEmpty() && DIGITS_ONLY.matcher(trimmed).matches()) {
            return SearchResult.digits();
        }
        String q = normalize(trimmed);
        if (q.isEmpty()) {
            return SearchResult.of(List.of());
        }
        Set<String> liveSlugs = towns.liveSlugs();

        List<Hit> exact = new ArrayList<>();
        List<Place> others = new ArrayList<>();
        for (Place p : places.places()) {
            boolean isExact = p.searchName().equals(q);
            if (isExact) {
                exact.add(toHit(p, liveSlugs, true));
            } else if (matches(q, p.searchName())) {
                others.add(p);
            }
        }
        // Non-exact matches: live towns first, then by name. The cap applies to this group only (REQ-02).
        others.sort(Comparator
            .comparing((Place p) -> !liveSlugs.contains(p.slug()))
            .thenComparing(Place::name));

        List<Hit> result = new ArrayList<>(exact);
        for (Place p : others) {
            if (result.size() - exact.size() >= MAX_RESULTS) {
                break;
            }
            result.add(toHit(p, liveSlugs, false));
        }
        return SearchResult.of(result);
    }

    /** A place by slug, for the coming-soon decision. */
    public Optional<Place> bySlug(String slug) {
        return places.bySlug(slug);
    }

    private static Hit toHit(Place p, Set<String> liveSlugs, boolean exact) {
        return new Hit(p.slug(), p.name(), p.state(), liveSlugs.contains(p.slug()), exact);
    }
}
