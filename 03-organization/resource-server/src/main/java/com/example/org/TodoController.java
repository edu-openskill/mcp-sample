package com.example.org;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/todos")
public class TodoController {

    private final TodoRepository repository;

    public TodoController(TodoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Todo> list() {
        return repository.findAll(AuthContext.getOrThrow());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> get(@PathVariable Long id) {
        return repository.findById(AuthContext.getOrThrow(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Todo add(@RequestBody AddRequest request) {
        return repository.add(AuthContext.getOrThrow(), request.title(), request.memo());
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Todo> complete(@PathVariable Long id) {
        return repository.complete(AuthContext.getOrThrow(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Todo> search(@RequestParam(value = "q", required = false) String query) {
        return repository.search(AuthContext.getOrThrow(), query);
    }

    public record AddRequest(String title, String memo) {}
}
