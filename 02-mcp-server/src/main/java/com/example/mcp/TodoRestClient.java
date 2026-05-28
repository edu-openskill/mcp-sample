package com.example.mcp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class TodoRestClient {

    private final RestClient client;

    public TodoRestClient(@Value("${resource-server.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<TodoView> list() {
        return client.get()
                .uri("/todos")
                .retrieve()
                .body(new ParameterizedTypeReference<List<TodoView>>() {});
    }

    public Optional<TodoView> get(long id) {
        TodoView body = client.get()
                .uri("/todos/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> { /* swallow */ })
                .body(TodoView.class);
        return Optional.ofNullable(body);
    }

    public TodoView add(String title, String memo) {
        return client.post()
                .uri("/todos")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(new AddBody(title, memo))
                .retrieve()
                .body(TodoView.class);
    }

    public Optional<TodoView> complete(long id) {
        TodoView body = client.patch()
                .uri("/todos/{id}/complete", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> { })
                .body(TodoView.class);
        return Optional.ofNullable(body);
    }

    public List<TodoView> search(String query) {
        return client.get()
                .uri(uri -> uri.path("/todos/search").queryParam("q", query).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<TodoView>>() {});
    }

    record AddBody(String title, String memo) {}
}
