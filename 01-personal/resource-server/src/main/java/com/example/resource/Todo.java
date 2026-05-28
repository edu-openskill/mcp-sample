package com.example.resource;

public record Todo(
        Long id,
        String title,
        String memo,
        boolean completed
) {
    public Todo complete() {
        return new Todo(id, title, memo, true);
    }
}
