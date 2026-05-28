package com.example.org;

public record Todo(
        Long id,
        String userId,
        String title,
        String memo,
        boolean completed
) {
    public Todo complete() {
        return new Todo(id, userId, title, memo, true);
    }
}
