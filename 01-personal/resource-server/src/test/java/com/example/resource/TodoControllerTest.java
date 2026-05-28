package com.example.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class TodoControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void listEmpty_returnsEmptyArray() throws Exception {
        mvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void addAndGet_roundTrip() throws Exception {
        mvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title": "Learn MCP", "memo": "today"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Learn MCP"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void completeFlow() throws Exception {
        String body = mvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title": "to complete", "memo": "x"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = new ObjectMapper().readTree(body).get("id").asLong();

        mvc.perform(patch("/todos/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void searchEndpoint_respondsOk() throws Exception {
        mvc.perform(get("/todos/search").param("q", "mcp"))
                .andExpect(status().isOk());
    }

    @Test
    void getMissingId_returns404() throws Exception {
        mvc.perform(get("/todos/{id}", 999_999))
                .andExpect(status().isNotFound());
    }
}
