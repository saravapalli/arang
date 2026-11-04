package com.arang.intake.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.arang.intake.geo.PlaceSearchService.Hit;
import com.arang.intake.geo.PlaceSearchService.SearchResult;
import com.arang.intake.town.TownRegistry;

/**
 * TC-P2.1-014..017 (ARANG-TTP-P2-1): word-start matching, normalization, digits-only hint, exact-first ranking and
 * the cap. Proves SDD-P2.1 task T6. Pure unit test with a fixed place list.
 */
class PlaceSearchServiceTest {

    private TownRegistry towns;

    private PlaceSearchService serviceWith(List<Place> list, Set<String> live) {
        GeoPlaceSource geo = Mockito.mock(GeoPlaceSource.class);
        when(geo.isUp()).thenReturn(true);
        when(geo.places()).thenReturn(list);
        towns = Mockito.mock(TownRegistry.class);
        when(towns.liveSlugs()).thenReturn(live);
        return new PlaceSearchService(geo, towns);
    }

    private static Place place(String name, String slug) {
        return new Place("id-" + slug, name, PlaceSearchService.normalize(name), "town", "MA", slug, 42.0, -71.0);
    }

    @Test
    void wordStartMatchingAndNormalization() {
        PlaceSearchService s = serviceWith(List.of(
            place("Ashland", "ashland-ma"),
            place("North Andover", "north-andover-ma"),
            place("Andover", "andover-ma")), Set.of("ashland-ma"));

        assertThat(slugs(s.search("ash"))).contains("ashland-ma");
        assertThat(slugs(s.search("ASH"))).contains("ashland-ma");
        assertThat(slugs(s.search("Ashland, MA"))).containsExactly("ashland-ma");
        assertThat(slugs(s.search("north and"))).contains("north-andover-ma");
        assertThat(slugs(s.search("andover north"))).contains("north-andover-ma");
        assertThat(slugs(s.search("land"))).doesNotContain("ashland-ma");  // middle of a word does not match
    }

    @Test
    void digitsOnlyReturnsHintNotResults() {
        PlaceSearchService s = serviceWith(List.of(place("Ashland", "ashland-ma")), Set.of("ashland-ma"));
        assertThat(s.search("01721").digitsHint()).isTrue();
        assertThat(s.search("017").digitsHint()).isTrue();
        assertThat(s.search("01721-1234").digitsHint()).isTrue();
        assertThat(s.search("ashland").digitsHint()).isFalse();
    }

    @Test
    void emptyAfterNormalizationReturnsNoResults() {
        PlaceSearchService s = serviceWith(List.of(place("Ashland", "ashland-ma")), Set.of("ashland-ma"));
        SearchResult r = s.search("ma");
        assertThat(r.digitsHint()).isFalse();
        assertThat(r.results()).isEmpty();
    }

    @Test
    void exactMatchIsNeverDroppedByTheCap() {
        // 12 "New..." names; a NOT-LIVE exact "Newton" must still be returned.
        List<Place> list = new java.util.ArrayList<>(List.of(
            place("Newton", "newton-ma"),
            place("New Ashford", "new-ashford-ma"), place("New Bedford", "new-bedford-ma"),
            place("New Braintree", "new-braintree-ma"), place("New Marlborough", "new-marlborough-ma"),
            place("New Salem", "new-salem-ma"), place("Newbury", "newbury-ma"),
            place("Newburyport", "newburyport-ma"), place("Newton Centre", "newton-centre-ma"),
            place("Newton Upper Falls", "newton-upper-falls-ma"), place("Needham", "needham-ma"),
            place("Newfield", "newfield-ma")));
        PlaceSearchService s = serviceWith(list, Set.of());  // none live
        SearchResult r = s.search("newton");
        Hit first = r.results().get(0);
        assertThat(first.slug()).isEqualTo("newton-ma");
        assertThat(first.exact()).isTrue();
    }

    @Test
    void liveTownsRankBeforeOthersAndCapApplies() {
        List<Place> list = new java.util.ArrayList<>();
        list.add(place("Boxford", "boxford-ma"));
        for (int i = 0; i < 10; i++) {
            list.add(place("Box Town " + i, "box-town-" + i + "-ma"));
        }
        PlaceSearchService s = serviceWith(list, Set.of("boxford-ma"));
        SearchResult r = s.search("box");
        assertThat(r.results().get(0).slug()).isEqualTo("boxford-ma");       // live first
        assertThat(r.results().get(0).live()).isTrue();
        assertThat(r.results()).hasSize(PlaceSearchService.MAX_RESULTS);     // no exact match here: capped at 8
    }

    private static List<String> slugs(SearchResult r) {
        return r.results().stream().map(Hit::slug).toList();
    }
}
