package com.arang.intake.extract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Pure unit tests for the shared LLM-reply JSON parser — no network, provider-agnostic. */
class ExtractionJsonParserTest {

    @Test
    void systemPromptAnchorsToTodaysDate() {
        String p = ExtractionJsonParser.systemPrompt(java.time.LocalDate.of(2026, 10, 26));
        assertThat(p).contains("today is Monday, 2026-10-26"); // 2026-10-26 is a Monday
        assertThat(p).contains("Resolve relative dates");
        assertThat(p).contains("Never use a past year");
    }

    @Test
    void parsesCleanJson() {
        String content = """
            {"relevant":true,"title":"Live Jazz","starts_at":"2026-11-06T20:00","location":"The Cellar",
             "event_type":"concert","confidence":{"title":0.9,"starts_at":0.8,"location":0.7}}
            """;
        ExtractedFields f = ExtractionJsonParser.parse(content);
        assertThat(f.title()).isEqualTo("Live Jazz");
        assertThat(f.startsAt()).isNotNull();
        assertThat(f.location()).isEqualTo("The Cellar");
        assertThat(f.eventType()).isEqualTo("concert");
        assertThat(f.titleConfidence()).isEqualTo(0.9);
    }

    @Test
    void parsesJsonWrappedInCodeFencesAndProse() {
        String content = """
            Sure! Here is the data:
            ```json
            {"relevant":true,"title":"Open Mic","starts_at":null,"location":null,"event_type":"other",
             "confidence":{"title":0.6}}
            ```
            """;
        ExtractedFields f = ExtractionJsonParser.parse(content);
        assertThat(f.title()).isEqualTo("Open Mic");
        assertThat(f.startsAt()).isNull();
        assertThat(f.eventType()).isEqualTo("other");
    }

    @Test
    void nullAndBlankFieldsBecomeNull() {
        String content = "{\"relevant\":true,\"title\":null,\"location\":\"\",\"starts_at\":\"not-a-date\"}";
        ExtractedFields f = ExtractionJsonParser.parse(content);
        assertThat(f.title()).isNull();
        assertThat(f.location()).isNull();
        assertThat(f.startsAt()).isNull();
        assertThat(f.hasAnyField()).isFalse();
    }

    @Test
    void garbageContentYieldsEmpty() {
        assertThat(ExtractionJsonParser.parse("I could not understand that.").hasAnyField()).isFalse();
        assertThat(ExtractionJsonParser.parse("").hasAnyField()).isFalse();
        assertThat(ExtractionJsonParser.parse(null).hasAnyField()).isFalse();
    }

    @Test
    void classifiesIrrelevantMessage() {
        String content = "{\"relevant\":false,\"reason\":\"just a greeting\",\"title\":null}";
        ExtractedFields f = ExtractionJsonParser.parse(content);
        assertThat(f.relevant()).isFalse();
        assertThat(f.irrelevantReason()).isEqualTo("just a greeting");
        assertThat(f.hasAnyField()).isFalse();
    }

    @Test
    void relevantEventIsMarkedRelevant() {
        String content = "{\"relevant\":true,\"title\":\"Gala\",\"confidence\":{\"title\":0.9}}";
        ExtractedFields f = ExtractionJsonParser.parse(content);
        assertThat(f.relevant()).isTrue();
        assertThat(f.title()).isEqualTo("Gala");
    }

    @Test
    void parsesAttributesAndRawText() {
        String content = """
            {"relevant":true,"title":"Rhythm Bollywood",
             "attributes":{"contact_name":"Heena","phone":"773-610-8868","instagram":"@rhythmbollywood","blank":""},
             "raw_text":"Rhythm Bollywood Dance Classes All Ages"}
            """;
        ExtractedFields f = ExtractionJsonParser.parse(content);
        assertThat(f.attributes()).containsEntry("contact_name", "Heena")
            .containsEntry("phone", "773-610-8868")
            .containsEntry("instagram", "@rhythmbollywood");
        assertThat(f.attributes()).doesNotContainKey("blank"); // empty values skipped
        assertThat(f.rawText()).contains("Rhythm Bollywood Dance Classes");
        assertThat(f.hasAnyField()).isTrue();
    }
}
