package com.example.server.auth.dto;

import com.example.server.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса на регистрацию пользователя.
 *
 * @param email email нового пользователя
 * @param password пароль пользователя
 * @param role роль пользователя в системе
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role
) {
}
