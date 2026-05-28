package com.example.mcp;

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
    void listTodos_delegatesToRestClient() {
        TodoView t = new TodoView(1L, "a", "b", false);
        when(restClient.list()).thenReturn(List.of(t));

        List<TodoView> result = tools.listTodos();

        assertEquals(1, result.size());
        verify(restClient).list();
    }

    @Test
    void getTodo_returnsNotFoundMessageWhenAbsent() {
        when(restClient.get(99L)).thenReturn(Optional.empty());

        TodoTools.GetResult result = tools.getTodo(99L);

        assertFalse(result.found());
        assertNull(result.todo());
    }

    @Test
    void getTodo_returnsTodoWhenPresent() {
        TodoView t = new TodoView(1L, "x", "y", false);
        when(restClient.get(1L)).thenReturn(Optional.of(t));

        TodoTools.GetResult result = tools.getTodo(1L);

        assertTrue(result.found());
        assertEquals(1L, result.todo().id());
    }

    @Test
    void addTodo_delegatesToRestClient() {
        TodoView added = new TodoView(5L, "new", "memo", false);
        when(restClient.add("new", "memo")).thenReturn(added);

        TodoView result = tools.addTodo("new", "memo");

        assertEquals(5L, result.id());
        verify(restClient).add("new", "memo");
    }

    @Test
    void completeTodo_returnsFlagWhenAbsent() {
        when(restClient.complete(404L)).thenReturn(Optional.empty());
        TodoTools.CompleteResult result = tools.completeTodo(404L);
        assertFalse(result.found());
    }

    @Test
    void completeTodo_returnsCompletedTodo() {
        TodoView completed = new TodoView(1L, "x", "y", true);
        when(restClient.complete(1L)).thenReturn(Optional.of(completed));

        TodoTools.CompleteResult result = tools.completeTodo(1L);

        assertTrue(result.found());
        assertTrue(result.todo().completed());
    }

    @Test
    void searchTodos_delegates() {
        when(restClient.search("mcp")).thenReturn(List.of());
        List<TodoView> result = tools.searchTodos("mcp");
        assertNotNull(result);
        verify(restClient).search("mcp");
    }
}
