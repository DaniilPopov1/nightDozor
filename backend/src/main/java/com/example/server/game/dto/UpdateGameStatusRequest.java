package com.example.server.game.dto;

import com.example.server.game.entity.GameStatus;
import jakarta.validation.constraints.NotNull;

/**
 * DTO запроса на изменение статуса игры.
 *
 * @param status новый статус игры
 */
public record UpdateGameStatusRequest(
        @NotNull GameStatus status
) {
}
