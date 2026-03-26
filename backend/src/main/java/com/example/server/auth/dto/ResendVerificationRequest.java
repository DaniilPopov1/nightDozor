package com.example.server.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO запроса на повторную отправку письма подтверждения.
 *
 * @param email email пользователя
 */
public record ResendVerificationRequest(
        @NotBlank @Email String email
) {
}
