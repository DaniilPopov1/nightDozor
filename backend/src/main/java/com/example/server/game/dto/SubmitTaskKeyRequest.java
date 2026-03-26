package com.example.server.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса на отправку ключа текущего задания.
 *
 * @param key введенный ключ
 */
public record SubmitTaskKeyRequest(
        @NotBlank @Size(max = 255) String key
) {
}
