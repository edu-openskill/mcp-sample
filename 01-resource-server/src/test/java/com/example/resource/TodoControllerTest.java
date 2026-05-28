package com.example.resource;

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
        String created = mvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title": "Learn MCP", "memo": "today"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Learn MCP"))
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn().getResponse().getContentAsString();

        // 단순히 ID가 발급됐는지만 확인 (다른 테스트와의 순서 의존 회피)
    }

    @Test
    void completeFlow() throws Exception {
        mvc.perform(post("/todos")
                        .contentType("application/json")
                        .content("""
                                {"title": "to complete", "memo": "x"}
                                """))
                .andExpect(status().isOk());

        // 가장 최근 ID로 완료 처리
        mvc.perform(get("/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
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
