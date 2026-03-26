package com.example.server.auth.event;

/**
 * Событие успешной регистрации пользователя.
 * Используется для отправки письма подтверждения после коммита транзакции.
 *
 * @param userId идентификатор пользователя
 * @param email email пользователя
 * @param token токен подтверждения
 */
public record UserRegisteredEvent(
        Long userId,
        String email,
        String token
) {
}
