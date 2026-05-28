package com.example.org;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TodoRepositoryTest {

    private TodoRepository repo;

    @BeforeEach
    void setUp() {
        repo = new TodoRepository();
    }

    @Test
    void add_assignsPerUserId() {
        Todo a1 = repo.add("alice", "first", "m");
        Todo b1 = repo.add("bob", "first", "m");

        assertEquals(1L, a1.id());
        assertEquals(1L, b1.id());  // alice와 bob은 각자 1번부터
        assertEquals("alice", a1.userId());
        assertEquals("bob", b1.userId());
    }

    @Test
    void findAll_returnsOnlyOwnTodos() {
        repo.add("alice", "a1", null);
        repo.add("alice", "a2", null);
        repo.add("bob", "b1", null);

        assertEquals(2, repo.findAll("alice").size());
        assertEquals(1, repo.findAll("bob").size());
    }

    @Test
    void findById_isolatesByUser() {
        Todo a = repo.add("alice", "x", null);
        Todo b = repo.add("bob", "y", null);

        // alice가 1번 조회 → alice의 1번
        Optional<Todo> aliceView = repo.findById("alice", a.id());
        assertTrue(aliceView.isPresent());
        assertEquals("alice", aliceView.get().userId());

        // bob이 1번 조회 → bob의 1번 (alice 것 X)
        Optional<Todo> bobView = repo.findById("bob", b.id());
        assertTrue(bobView.isPresent());
        assertEquals("bob", bobView.get().userId());
    }

    @Test
    void findById_returnsEmptyForOtherUser() {
        Todo a = repo.add("alice", "x", null);
        Optional<Todo> bobView = repo.findById("bob", a.id());
        assertTrue(bobView.isEmpty(),
                "bob은 alice의 todo를 못 봐야 함");
    }

    @Test
    void complete_isolatesByUser() {
        Todo a = repo.add("alice", "x", null);
        Optional<Todo> bobComplete = repo.complete("bob", a.id());
        assertTrue(bobComplete.isEmpty(),
                "bob은 alice의 todo를 완료처리 못 함");

        Optional<Todo> aliceComplete = repo.complete("alice", a.id());
        assertTrue(aliceComplete.isPresent());
        assertTrue(aliceComplete.get().completed());
    }

    @Test
    void search_isolatesByUser() {
        repo.add("alice", "MCP study", null);
        repo.add("bob", "MCP design", null);

        List<Todo> aliceResults = repo.search("alice", "MCP");
        assertEquals(1, aliceResults.size());
        assertEquals("alice", aliceResults.get(0).userId());
    }
}
