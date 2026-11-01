package com.arang.intake.form;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** T8 — direct-entry web form (REQ-14). */
@SpringBootTest
@ActiveProfiles("test")
class FormControllerTest {

    @Autowired private WebApplicationContext context;
    @Autowired private EventDao events;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        events.deleteAll();
    }

    @Test
    void getRendersForm() throws Exception {
        mvc.perform(get("/submit")).andExpect(status().isOk());
    }

    @Test
    void postCreatesUserOriginDraftAndRedirectsToReview() throws Exception {
        mvc.perform(post("/submit")
                .param("title", "Open Mic")
                .param("location", "The Bar")
                .param("postalCode", "01721")
                .param("details", "come by https://ex.com/mic"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/review/")));

        assertThat(events.count()).isEqualTo(1);
        Event e = events.findAll().get(0);
        assertThat(e.getOriginType()).isEqualTo("user");   // REQ-14
        assertThat(e.getSubmittedBy()).isNull();            // no WhatsApp id
        assertThat(e.getStatus()).isEqualTo("draft");
        assertThat(e.getTitle()).isEqualTo("Open Mic");
        assertThat(e.getPostalCode()).isEqualTo("01721");
        assertThat(e.getEditToken()).isNotNull();           // reviewable via token link
    }
}
