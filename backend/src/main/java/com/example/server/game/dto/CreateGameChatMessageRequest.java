package com.example.server.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса на отправку сообщения в игровой чат.
 *
 * @param text текст сообщения
 */
public record CreateGameChatMessageRequest(
        @NotBlank @Size(max = 4000) String text
) {
}
