package com.example.rag.security;

public record CurrentUser(
        Long id,
        String username,
        String nickname,
        String role,
        String status
) {
}
