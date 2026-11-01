package com.arang.intake.console;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.arang.intake.dao.EventDao;

/** Producer Console: page renders + message endpoint ingests and returns a review link. */
@SpringBootTest
@ActiveProfiles("test")
class ProducerConsoleControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private EventDao events;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        events.deleteAll();
    }

    @Test
    void consolePageRenders() throws Exception {
        mvc.perform(get("/console")).andExpect(status().isOk());
    }

    @Test
    void sendingAMessageCreatesDraftAndReturnsReviewLink() throws Exception {
        mvc.perform(post("/api/producer/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Jazz Friday 8pm tickets https://example.com/j\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.reviewPath").value(org.hamcrest.Matchers.startsWith("/review/")))
            .andExpect(jsonPath("$.reply").value(org.hamcrest.Matchers.containsString("Review & edit")));

        assertThat(events.count()).isEqualTo(1);
    }

    @Test
    void blankMessageReturnsError() throws Exception {
        mvc.perform(post("/api/producer/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"  \"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void statusReportsNotEnrichedForFreshDraft() throws Exception {
        // send a message; with the rule-based extractor (test profile) nothing gets enriched
        String resp = mvc.perform(post("/api/producer/message")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"some text https://e.com\"}"))
            .andReturn().getResponse().getContentAsString();
        long id = com.jayway.jsonpath.JsonPath.parse(resp).read("$.eventId", Long.class);

        mvc.perform(get("/api/producer/message/{id}/status", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.enriched").value(false)); // rule-based extractor fills nothing
    }

    @Test
    void statusForUnknownEventReturnsNotOk() throws Exception {
        mvc.perform(get("/api/producer/message/{id}/status", 999999L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void uploadingImageCreatesImageDraftAndReturnsReviewLink() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "flyer.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2, 3});

        mvc.perform(multipart("/api/producer/image").file(file).param("caption", "jazz night"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.reviewPath").value(org.hamcrest.Matchers.startsWith("/review/")));

        assertThat(events.count()).isEqualTo(1);
        assertThat(events.findAll().get(0).getSourceType()).isEqualTo("image");
    }

    @Test
    void uploadingEmptyFileReturnsError() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);
        mvc.perform(multipart("/api/producer/image").file(empty))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(false));
    }
}
