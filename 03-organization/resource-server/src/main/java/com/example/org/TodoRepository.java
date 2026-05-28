package com.example.org;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class TodoRepository {

    /** userId -> (todoId -> todo) */
    private final Map<String, Map<Long, Todo>> stores = new ConcurrentHashMap<>();

    /** userId별 ID 시퀀스 */
    private final Map<String, AtomicLong> sequences = new ConcurrentHashMap<>();

    public Todo add(String userId, String title, String memo) {
        long id = sequences.computeIfAbsent(userId, k -> new AtomicLong(0)).incrementAndGet();
        Todo todo = new Todo(id, userId, title, memo, false);
        stores.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(id, todo);
        return todo;
    }

    public Optional<Todo> findById(String userId, Long id) {
        return Optional.ofNullable(storeOf(userId).get(id));
    }

    public List<Todo> findAll(String userId) {
        return storeOf(userId).values().stream()
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }

    public Optional<Todo> complete(String userId, Long id) {
        return findById(userId, id).map(t -> {
            Todo updated = t.complete();
            storeOf(userId).put(id, updated);
            return updated;
        });
    }

    public List<Todo> search(String userId, String query) {
        String q = query == null ? "" : query.toLowerCase();
        return findAll(userId).stream()
                .filter(t -> {
                    String title = t.title() == null ? "" : t.title().toLowerCase();
                    String memo = t.memo() == null ? "" : t.memo().toLowerCase();
                    return title.contains(q) || memo.contains(q);
                })
                .toList();
    }

    private Map<Long, Todo> storeOf(String userId) {
        return stores.getOrDefault(userId, Map.of());
    }
}
