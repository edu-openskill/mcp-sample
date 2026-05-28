package com.example.external;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TodoTools {

    private static final String FAKE_NOTE =
            "참고: JSONPlaceholder는 데모 API라 POST/PATCH가 실제 저장되지 않습니다. 응답만 돌아옵니다.";

    private final JsonPlaceholderClient client;

    public TodoTools(JsonPlaceholderClient client) {
        this.client = client;
    }

    @Tool(name = "list_todos", description = "JSONPlaceholder fake Todo 목록을 반환합니다 (기본 20건).")
    public List<TodoView> listTodos() {
        return client.listAll(20);
    }

    @Tool(name = "list_todos_by_user",
          description = "특정 사용자(userId)의 할일 목록을 반환합니다.")
    public List<TodoView> listTodosByUser(
            @ToolParam(description = "사용자 ID (1~10)", required = true) Long userId) {
        return client.listByUser(userId);
    }

    @Tool(name = "get_todo", description = "ID로 할일 한 건을 조회합니다.")
    public GetResult getTodo(
            @ToolParam(description = "할일 ID (1~200)", required = true) Long id) {
        return client.get(id)
                .map(t -> new GetResult(true, t))
                .orElseGet(() -> new GetResult(false, null));
    }

    @Tool(name = "add_todo",
          description = "새 할일을 추가합니다. JSONPlaceholder는 fake API라 실제 저장되지 않습니다.")
    public AddResult addTodo(
            @ToolParam(description = "제목", required = true) String title,
            @ToolParam(description = "사용자 ID", required = true) Long userId) {
        TodoView created = client.add(title, userId);
        return new AddResult(created, FAKE_NOTE);
    }

    @Tool(name = "complete_todo",
          description = "할일을 완료 처리합니다. JSONPlaceholder는 fake API라 실제 저장되지 않습니다.")
    public CompleteResult completeTodo(
            @ToolParam(description = "할일 ID", required = true) Long id) {
        TodoView completed = client.complete(id);
        return new CompleteResult(completed, FAKE_NOTE);
    }

    @Tool(name = "search_todos",
          description = "제목에 키워드가 포함된 할일을 검색합니다 (전체 200건 중).")
    public List<TodoView> searchTodos(
            @ToolParam(description = "검색 키워드", required = true) String query) {
        String q = query == null ? "" : query.toLowerCase();
        return client.listAll(200).stream()
                .filter(t -> t.title() != null && t.title().toLowerCase().contains(q))
                .toList();
    }

    public record GetResult(boolean found, TodoView todo) {}
    public record AddResult(TodoView todo, String note) {}
    public record CompleteResult(TodoView todo, String note) {}
}
