package com.arang.intake.discovery;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.arang.intake.dao.EventDao;

/**
 * DiscoveryController (ARANG-TTP-P2): TC-05 (from>to → 400 error body), TC-10 (envelope shape),
 * TC-09-adjacent (categories endpoint returns the seeded vocabulary).
 */
@SpringBootTest
@ActiveProfiles("test")
class DiscoveryControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired EventDao events;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        events.deleteAll();
    }

    @Test
    void invertedWindowReturns400WithErrorBody() throws Exception { // TC-05
        mvc.perform(get("/api/discovery").param("from", "2026-11-05").param("to", "2026-11-01"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("invalid_window"));
    }

    @Test
    void envelopeHasWindowAreaAndResults() throws Exception { // TC-10
        mvc.perform(get("/api/discovery").param("location", "01721"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.window.from").exists())
            .andExpect(jsonPath("$.window.to").exists())
            .andExpect(jsonPath("$.area.postal").value("01721"))
            .andExpect(jsonPath("$.area.radiusMiles").value(15))
            .andExpect(jsonPath("$.truncated").isBoolean())
            .andExpect(jsonPath("$.interestFilterMatchedNothing").isBoolean())
            .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void categoriesEndpointReturnsSeededVocabulary() throws Exception { // TC-09 adjacent
        mvc.perform(get("/api/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[?(@ == 'music')]").exists())
            .andExpect(jsonPath("$[?(@ == 'family')]").exists());
    }

    @Test
    void eventDetailPublished200DraftAndUnknown404() throws Exception { // TC-09
        com.arang.intake.model.Event pub = new com.arang.intake.model.Event();
        pub.setTitle("Published detail");
        pub.setStatus("published");
        pub.setSourceType("text");
        pub.setOriginType("user");
        pub = events.insert(pub);

        com.arang.intake.model.Event draft = new com.arang.intake.model.Event();
        draft.setTitle("Draft detail");
        draft.setStatus("draft");
        draft.setSourceType("text");
        draft.setOriginType("user");
        draft = events.insert(draft);

        mvc.perform(get("/api/events/" + pub.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Published detail"));

        mvc.perform(get("/api/events/" + draft.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/events/99999999")).andExpect(status().isNotFound());
    }
}
