package com.example.external;

public record TodoView(
        Long userId,
        Long id,
        String title,
        boolean completed
) {}
