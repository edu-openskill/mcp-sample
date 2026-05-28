package com.example.orgmcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TodoTools {

    private final TodoRestClient restClient;

    public TodoTools(TodoRestClient restClient) {
        this.restClient = restClient;
    }

    @Tool(name = "list_todos", description = "현재 사용자의 할일 목록을 반환합니다.")
    public List<TodoView> listTodos() {
        return restClient.list();
    }

    @Tool(name = "get_todo", description = "ID로 할일 한 건을 조회합니다.")
    public GetResult getTodo(
            @ToolParam(description = "할일 ID", required = true) Long id) {
        return restClient.get(id)
                .map(t -> new GetResult(true, t))
                .orElseGet(() -> new GetResult(false, null));
    }

    @Tool(name = "add_todo", description = "현재 사용자에게 새 할일을 추가합니다.")
    public TodoView addTodo(
            @ToolParam(description = "제목", required = true) String title,
            @ToolParam(description = "메모(선택)", required = false) String memo) {
        return restClient.add(title, memo);
    }

    @Tool(name = "complete_todo", description = "현재 사용자의 할일을 완료 처리합니다.")
    public CompleteResult completeTodo(
            @ToolParam(description = "할일 ID", required = true) Long id) {
        return restClient.complete(id)
                .map(t -> new CompleteResult(true, t))
                .orElseGet(() -> new CompleteResult(false, null));
    }

    @Tool(name = "search_todos", description = "현재 사용자의 할일 중 키워드 검색.")
    public List<TodoView> searchTodos(
            @ToolParam(description = "검색 키워드", required = true) String query) {
        return restClient.search(query);
    }

    public record GetResult(boolean found, TodoView todo) {}
    public record CompleteResult(boolean found, TodoView todo) {}
}
