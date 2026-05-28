package com.example.org;

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

    @Autowired MockMvc mvc;

    @Test
    void missingAuthHeader_returns401() throws Exception {
        mvc.perform(get("/todos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mvc.perform(get("/todos").header("Authorization", "Bearer wrong-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aliceCanListEmpty() throws Exception {
        mvc.perform(get("/todos").header("Authorization", "Bearer alice-token"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void aliceAndBobAreIsolated() throws Exception {
        // alice가 1건 추가
        mvc.perform(post("/todos")
                        .header("Authorization", "Bearer alice-token")
                        .contentType("application/json")
                        .content("""
                                {"title": "alice's task", "memo": null}
                                """))
                .andExpect(status().isCreated());

        // bob의 목록은 비어있어야
        mvc.perform(get("/todos").header("Authorization", "Bearer bob-token"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // alice의 목록은 1건
        mvc.perform(get("/todos").header("Authorization", "Bearer alice-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value("alice"));
    }

    @Test
    void completeFlow() throws Exception {
        String body = mvc.perform(post("/todos")
                        .header("Authorization", "Bearer alice-token")
                        .contentType("application/json")
                        .content("""
                                {"title": "to complete", "memo": null}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = new ObjectMapper().readTree(body).get("id").asLong();

        mvc.perform(patch("/todos/{id}/complete", id)
                        .header("Authorization", "Bearer alice-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }
}
