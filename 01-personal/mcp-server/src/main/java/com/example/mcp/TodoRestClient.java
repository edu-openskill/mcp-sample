package com.example.mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/**
 * Resource Server(별도 Spring Boot REST API)를 호출하는 HTTP 클라이언트.
 *
 * 역할: MCP 서버는 비즈니스 데이터(Todo)를 직접 가지고 있지 않다.
 *       실제 데이터는 Resource Server가 가지고 있고, 이 클래스가 그쪽 REST 엔드포인트를
 *       대신 호출해서 결과를 받아온다.
 *
 * 중요: 여기 적힌 경로("/todos", "/todos/{id}/complete" 등)와 HTTP 메서드는
 *       Resource Server의 REST 스펙을 보고 개발자가 "직접" 옮겨 적은 것이다.
 *       MCP에는 REST 스펙을 자동으로 알아내는 표준이 없다.
 *       (OpenAPI가 있다면 코드 생성으로 자동화는 가능하지만, MCP 자체 기능은 아니다.)
 */
@Component
public class TodoRestClient {

    // Spring 6의 동기식 HTTP 클라이언트. RestTemplate의 후계자.
    private final RestClient client;

    /**
     * application.yml 의 `resource-server.base-url` 값을 주입받아
     * 모든 요청의 베이스 URL로 고정한다. (예: http://localhost:8081)
     */
    public TodoRestClient(@Value("${resource-server.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    /** GET /todos — 전체 Todo 목록 조회. */
    public List<TodoView> list() {
        return client.get()
                .uri("/todos")
                .retrieve()
                // List<TodoView> 같은 제네릭 타입은 타입 소거 때문에
                // ParameterizedTypeReference로 감싸야 Jackson이 정확히 역직렬화한다.
                .body(new ParameterizedTypeReference<List<TodoView>>() {});
    }

    /**
     * GET /todos/{id} — 단건 조회.
     * 존재하지 않으면 Resource Server가 4xx를 반환하므로 Optional.empty()로 변환한다.
     */
    public Optional<TodoView> get(long id) {
        TodoView body = client.get()
                .uri("/todos/{id}", id)
                .retrieve()
                // 4xx가 떨어져도 예외를 던지지 않고 무시 → body가 null이 되어 Optional로 감싼다.
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> { /* swallow */ })
                .body(TodoView.class);
        return Optional.ofNullable(body);
    }

    /** POST /todos — 새 Todo 생성. 요청 본문은 아래 AddBody 레코드로 직렬화된다. */
    public TodoView add(String title, String memo) {
        return client.post()
                .uri("/todos")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new AddBody(title, memo))
                .retrieve()
                .body(TodoView.class);
    }

    /**
     * PATCH /todos/{id}/complete — 완료 처리.
     * 대상이 없으면 4xx → Optional.empty()로 변환 (get(id)와 같은 패턴).
     */
    public Optional<TodoView> complete(long id) {
        TodoView body = client.patch()
                .uri("/todos/{id}/complete", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> { })
                .body(TodoView.class);
        return Optional.ofNullable(body);
    }

    /** GET /todos/search?q=... — 키워드 검색. 쿼리 파라미터는 UriBuilder로 안전하게 인코딩한다. */
    public List<TodoView> search(String query) {
        return client.get()
                .uri(uri -> uri.path("/todos/search").queryParam("q", query).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<TodoView>>() {});
    }

    /** POST /todos 요청 본문 전용 DTO. 외부에 노출할 필요가 없어 package-private record로 둔다. */
    record AddBody(String title, String memo) {}
}
