package com.example.server.auth.event;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String token
) {
}
