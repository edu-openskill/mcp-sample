package com.example.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP "툴(Tool)" 정의 클래스.
 *
 * Spring AI는 부팅 시 빈을 스캔해서 @Tool 이 붙은 메서드들을 모아
 * MCP 프로토콜의 tools/list 응답으로 노출한다.
 * → Claude(MCP 클라이언트)는 이 목록을 받아보고 "어떤 툴이 있고, 파라미터가 뭔지" 알게 된다.
 *
 * 즉, 자동 발견되는 것은 "MCP 클라이언트 ← MCP 서버" 방향뿐이고,
 * "MCP 서버 ← Resource Server REST 스펙" 방향은 자동이 아니다.
 * 아래 메서드 하나하나가 TodoRestClient의 메서드(=REST 엔드포인트)와 1:1로
 * 개발자가 직접 매핑해 둔 결과다.
 */
@Component
public class TodoTools {

    // 실제 HTTP 호출은 RestClient 래퍼에 위임한다. 이 클래스는 "MCP 노출 계층"만 담당.
    private final TodoRestClient restClient;

    public TodoTools(TodoRestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @Tool 의 name/description 은 LLM이 어떤 툴을 쓸지 고를 때 보는 메타데이터다.
     * 설명을 모호하게 쓰면 LLM이 호출을 회피하거나 잘못 고르기 쉬우니, 구체적으로 적는다.
     */
    @Tool(name = "list_todos", description = "모든 할일(Todo) 목록을 반환합니다.")
    public List<TodoView> listTodos() {
        return restClient.list();
    }

    /**
     * @ToolParam 의 description/required 도 LLM이 인자를 채울 때 참고한다.
     * 반환 타입을 raw TodoView 가 아닌 GetResult(found, todo) 로 감싸는 이유:
     *   - "없음"을 null/예외 대신 명시적 boolean으로 표현해 LLM이 분기하기 쉽게 한다.
     */
    @Tool(name = "get_todo", description = "ID로 할일 한 건을 조회합니다.")
    public GetResult getTodo(
            @ToolParam(description = "할일 ID", required = true) Long id) {
        return restClient.get(id)
                .map(t -> new GetResult(true, t))
                .orElseGet(() -> new GetResult(false, null));
    }

    /** required=false 인 memo는 LLM이 생략 가능. 생략 시 null이 그대로 REST에 전달된다. */
    @Tool(name = "add_todo", description = "새 할일을 추가합니다. memo는 선택입니다.")
    public TodoView addTodo(
            @ToolParam(description = "제목", required = true) String title,
            @ToolParam(description = "메모(선택)", required = false) String memo) {
        return restClient.add(title, memo);
    }

    /** get_todo와 동일한 패턴: 대상이 없으면 found=false 로 응답. */
    @Tool(name = "complete_todo", description = "ID에 해당하는 할일을 완료 처리합니다.")
    public CompleteResult completeTodo(
            @ToolParam(description = "할일 ID", required = true) Long id) {
        return restClient.complete(id)
                .map(t -> new CompleteResult(true, t))
                .orElseGet(() -> new CompleteResult(false, null));
    }

    @Tool(name = "search_todos", description = "제목과 메모에서 키워드를 포함하는 할일을 검색합니다.")
    public List<TodoView> searchTodos(
            @ToolParam(description = "검색 키워드", required = true) String query) {
        return restClient.search(query);
    }

    /** "조회 성공 여부 + 데이터"를 함께 담는 응답 DTO. LLM 친화적 응답 모양을 만들기 위한 것. */
    public record GetResult(boolean found, TodoView todo) {}
    /** complete 호출 결과용. GetResult와 분리해 둠으로써 각 툴의 시그니처를 명확히 한다. */
    public record CompleteResult(boolean found, TodoView todo) {}
}
