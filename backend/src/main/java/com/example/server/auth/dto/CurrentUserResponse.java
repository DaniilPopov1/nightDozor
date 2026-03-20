package com.example.server.auth.dto;

import com.example.server.auth.entity.Role;

import java.time.Instant;

public record CurrentUserResponse(
        Long id,
        String email,
        Role role,
        boolean enabled,
        boolean accountNonLocked,
        Instant createdAt
) {
}
