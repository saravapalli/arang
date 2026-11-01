package com.arang.intake.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.arang.intake.model.Event;
import com.arang.intake.dao.EventDao;

/** T5 — token-gated review/edit page (REQ-09, REQ-10). */
@SpringBootTest
@ActiveProfiles("test")
class ReviewControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private EventDao events;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        events.deleteAll();
    }

    private Event draftWithToken(String token, LocalDateTime expiry) {
        Event e = new Event();
        e.setStatus("draft");
        e.setSourceType("text");
        e.setOriginType("user");
        e.setTitle("Jazz Night");
        e.setEditToken(token);
        e.setEditTokenExpiresAt(expiry);
        return events.insert(e);
    }

    @Test
    void getWithValidTokenRendersPrefilledFields() throws Exception {
        Event e = draftWithToken("tok-valid", LocalDateTime.now().plusHours(1));
        mvc.perform(get("/review/{id}", e.getId()).param("token", "tok-valid"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Jazz Night")));
    }

    @Test
    void getWithMissingTokenIs404() throws Exception {
        Event e = draftWithToken("tok-valid", LocalDateTime.now().plusHours(1));
        mvc.perform(get("/review/{id}", e.getId())).andExpect(status().isNotFound());
    }

    @Test
    void getWithWrongTokenIs404() throws Exception {
        Event e = draftWithToken("tok-valid", LocalDateTime.now().plusHours(1));
        mvc.perform(get("/review/{id}", e.getId()).param("token", "nope"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getWithExpiredTokenIs404() throws Exception {
        Event e = draftWithToken("tok-exp", LocalDateTime.now().minusMinutes(1));
        mvc.perform(get("/review/{id}", e.getId()).param("token", "tok-exp"))
            .andExpect(status().isNotFound());
    }

    @Test
    void getUnknownEventIs404() throws Exception {
        mvc.perform(get("/review/{id}", 999999L).param("token", "whatever"))
            .andExpect(status().isNotFound());
    }

    @Test
    void postSavesEditsAndSetsUpdatedValues() throws Exception {
        Event e = draftWithToken("tok-edit", LocalDateTime.now().plusHours(1));
        mvc.perform(post("/review/{id}", e.getId())
                .param("token", "tok-edit")
                .param("title", "Updated Title")
                .param("location", "Downtown Hall")
                .param("postalCode", "01721"))
            .andExpect(status().isOk());

        Event reloaded = events.findById(e.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Updated Title");
        assertThat(reloaded.getLocation()).isEqualTo("Downtown Hall");
        assertThat(reloaded.getPostalCode()).isEqualTo("01721");
    }

    @Test
    void postWithBadTokenIs404AndDoesNotEdit() throws Exception {
        Event e = draftWithToken("tok-edit", LocalDateTime.now().plusHours(1));
        mvc.perform(post("/review/{id}", e.getId())
                .param("token", "wrong")
                .param("title", "Hacked"))
            .andExpect(status().isNotFound());
        assertThat(events.findById(e.getId()).orElseThrow().getTitle()).isEqualTo("Jazz Night");
    }
}
