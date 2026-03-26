package com.example.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO запроса на вход в систему.
 *
 * @param email email пользователя
 * @param password пароль пользователя
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
