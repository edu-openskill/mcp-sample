package com.example.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TodoRepositoryTest {

    private TodoRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TodoRepository();
    }

    @Test
    void add_assignsIncrementingIds() {
        Todo a = repository.add("first", "memo1");
        Todo b = repository.add("second", "memo2");

        assertNotNull(a.id());
        assertNotNull(b.id());
        assertNotEquals(a.id(), b.id());
        assertEquals("first", a.title());
        assertFalse(a.completed());
    }

    @Test
    void findById_returnsEmptyWhenAbsent() {
        Optional<Todo> result = repository.findById(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void findById_returnsAddedTodo() {
        Todo added = repository.add("hello", "world");
        Optional<Todo> found = repository.findById(added.id());

        assertTrue(found.isPresent());
        assertEquals(added.id(), found.get().id());
        assertEquals("hello", found.get().title());
    }

    @Test
    void findAll_returnsAllAddedTodos() {
        repository.add("a", null);
        repository.add("b", null);
        repository.add("c", null);

        List<Todo> all = repository.findAll();
        assertEquals(3, all.size());
    }

    @Test
    void complete_marksTodoCompleted() {
        Todo added = repository.add("task", "memo");
        Optional<Todo> completed = repository.complete(added.id());

        assertTrue(completed.isPresent());
        assertTrue(completed.get().completed());
        assertEquals(added.id(), completed.get().id());
    }

    @Test
    void complete_returnsEmptyWhenAbsent() {
        assertTrue(repository.complete(404L).isEmpty());
    }

    @Test
    void search_matchesTitleAndMemoCaseInsensitive() {
        repository.add("Buy milk", "from market");
        repository.add("Read MCP spec", "this week");
        repository.add("Cook dinner", "spec recipe");

        List<Todo> result = repository.search("spec");
        assertEquals(2, result.size());
    }
}
