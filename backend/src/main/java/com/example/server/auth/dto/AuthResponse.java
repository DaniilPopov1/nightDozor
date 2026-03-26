package com.example.server.auth.dto;

import java.time.Instant;

/**
 * DTO ответа успешной аутентификации.
 *
 * @param accessToken JWT access token
 * @param tokenType тип токена для заголовка Authorization
 * @param expiresAt момент истечения токена
 * @param email email аутентифицированного пользователя
 * @param role роль пользователя
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        String email,
        String role
) {
}
