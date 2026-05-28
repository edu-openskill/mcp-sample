package com.example.orgmcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoToolsTest {

    private TodoRestClient restClient;
    private TodoTools tools;

    @BeforeEach
    void setUp() {
        restClient = mock(TodoRestClient.class);
        tools = new TodoTools(restClient);
    }

    @Test
    void listTodos_delegates() {
        TodoView t = new TodoView(1L, "alice", "a", null, false);
        when(restClient.list()).thenReturn(List.of(t));

        assertEquals(1, tools.listTodos().size());
        verify(restClient).list();
    }

    @Test
    void getTodo_returnsFoundFalseWhenAbsent() {
        when(restClient.get(99L)).thenReturn(Optional.empty());
        assertFalse(tools.getTodo(99L).found());
    }

    @Test
    void getTodo_returnsFoundWhenPresent() {
        TodoView t = new TodoView(1L, "alice", "x", null, false);
        when(restClient.get(1L)).thenReturn(Optional.of(t));
        TodoTools.GetResult r = tools.getTodo(1L);
        assertTrue(r.found());
        assertEquals(1L, r.todo().id());
    }

    @Test
    void addTodo_delegates() {
        TodoView created = new TodoView(5L, "alice", "new", "m", false);
        when(restClient.add("new", "m")).thenReturn(created);
        assertEquals(5L, tools.addTodo("new", "m").id());
    }

    @Test
    void completeTodo_returnsFlagWhenAbsent() {
        when(restClient.complete(404L)).thenReturn(Optional.empty());
        assertFalse(tools.completeTodo(404L).found());
    }

    @Test
    void completeTodo_returnsCompleted() {
        TodoView c = new TodoView(1L, "alice", "x", null, true);
        when(restClient.complete(1L)).thenReturn(Optional.of(c));
        TodoTools.CompleteResult r = tools.completeTodo(1L);
        assertTrue(r.found());
        assertTrue(r.todo().completed());
    }

    @Test
    void searchTodos_delegates() {
        when(restClient.search("mcp")).thenReturn(List.of());
        assertNotNull(tools.searchTodos("mcp"));
        verify(restClient).search("mcp");
    }
}
