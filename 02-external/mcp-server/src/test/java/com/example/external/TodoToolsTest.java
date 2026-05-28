package com.example.external;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoToolsTest {

    private JsonPlaceholderClient client;
    private TodoTools tools;

    @BeforeEach
    void setUp() {
        client = mock(JsonPlaceholderClient.class);
        tools = new TodoTools(client);
    }

    @Test
    void listTodos_passesLimitToClient() {
        when(client.listAll(20)).thenReturn(List.of(new TodoView(1L, 1L, "a", false)));

        List<TodoView> result = tools.listTodos();

        assertEquals(1, result.size());
        verify(client).listAll(20);
    }

    @Test
    void listTodosByUser_delegates() {
        when(client.listByUser(3L)).thenReturn(List.of());
        tools.listTodosByUser(3L);
        verify(client).listByUser(3L);
    }

    @Test
    void getTodo_returnsFoundFalseWhenAbsent() {
        when(client.get(404L)).thenReturn(Optional.empty());
        TodoTools.GetResult r = tools.getTodo(404L);
        assertFalse(r.found());
    }

    @Test
    void getTodo_returnsFoundWhenPresent() {
        TodoView t = new TodoView(1L, 1L, "x", false);
        when(client.get(1L)).thenReturn(Optional.of(t));
        TodoTools.GetResult r = tools.getTodo(1L);
        assertTrue(r.found());
        assertEquals(1L, r.todo().id());
    }

    @Test
    void addTodo_includesNoteAboutFakePersistence() {
        TodoView created = new TodoView(1L, 201L, "new", false);
        when(client.add("new", 1L)).thenReturn(created);

        TodoTools.AddResult result = tools.addTodo("new", 1L);

        assertEquals(201L, result.todo().id());
        assertTrue(result.note().contains("실제 저장되지 않습니다"),
                "JSONPlaceholder fake POST 경고를 응답에 포함해야 함");
    }

    @Test
    void completeTodo_includesFakePersistenceNote() {
        TodoView completed = new TodoView(1L, 1L, "x", true);
        when(client.complete(1L)).thenReturn(completed);

        TodoTools.CompleteResult result = tools.completeTodo(1L);

        assertTrue(result.todo().completed());
        assertTrue(result.note().contains("실제 저장되지 않습니다"));
    }

    @Test
    void searchTodos_filtersByTitleSubstring() {
        when(client.listAll(200)).thenReturn(List.of(
                new TodoView(1L, 1L, "buy milk", false),
                new TodoView(1L, 2L, "read MCP spec", false),
                new TodoView(2L, 3L, "milk shopping", true)
        ));

        List<TodoView> result = tools.searchTodos("milk");

        assertEquals(2, result.size());
    }
}
