package com.example.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class JsonPlaceholderClient {

    private final RestClient client;

    public JsonPlaceholderClient(@Value("${jsonplaceholder.base-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<TodoView> listAll(int limit) {
        return client.get()
                .uri(uri -> uri.path("/todos").queryParam("_limit", limit).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<TodoView>>() {});
    }

    public List<TodoView> listByUser(long userId) {
        return client.get()
                .uri(uri -> uri.path("/todos").queryParam("userId", userId).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<TodoView>>() {});
    }

    public Optional<TodoView> get(long id) {
        TodoView body = client.get()
                .uri("/todos/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> { })
                .body(TodoView.class);
        return Optional.ofNullable(body);
    }

    public TodoView add(String title, long userId) {
        return client.post()
                .uri("/todos")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AddBody(userId, title, false))
                .retrieve()
                .body(TodoView.class);
    }

    public TodoView complete(long id) {
        return client.patch()
                .uri("/todos/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PatchBody(true))
                .retrieve()
                .body(TodoView.class);
    }

    record AddBody(Long userId, String title, boolean completed) {}
    record PatchBody(boolean completed) {}
}
