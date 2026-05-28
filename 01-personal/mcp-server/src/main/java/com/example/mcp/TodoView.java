package com.example.mcp;

public record TodoView(
        Long id,
        String title,
        String memo,
        boolean completed
) {}
