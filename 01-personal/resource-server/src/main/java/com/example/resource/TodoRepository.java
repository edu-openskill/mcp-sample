package com.example.resource;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TodoRepository {

    private final Map<Long, Todo> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public Todo add(String title, String memo) {
        long id = sequence.incrementAndGet();
        Todo todo = new Todo(id, title, memo, false);
        store.put(id, todo);
        return todo;
    }

    public Optional<Todo> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Todo> findAll() {
        return store.values().stream()
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }

    public Optional<Todo> complete(Long id) {
        return findById(id).map(existing -> {
            Todo updated = existing.complete();
            store.put(id, updated);
            return updated;
        });
    }

    public List<Todo> search(String query) {
        String q = query == null ? "" : query.toLowerCase();
        return findAll().stream()
                .filter(t -> {
                    String title = t.title() == null ? "" : t.title().toLowerCase();
                    String memo = t.memo() == null ? "" : t.memo().toLowerCase();
                    return title.contains(q) || memo.contains(q);
                })
                .toList();
    }
}
