package com.example.orgmcp;

public record TodoView(
        Long id,
        String userId,
        String title,
        String memo,
        boolean completed
) {}
