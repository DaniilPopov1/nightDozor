package com.example.server.game.dto;

import java.time.Instant;

/**
 * DTO результата запуска игры.
 *
 * @param gameId идентификатор игры
 * @param gameTitle название игры
 * @param startedSessionsCount количество созданных игровых сессий
 * @param startedAt время запуска игры
 */
public record GameStartResponse(
        Long gameId,
        String gameTitle,
        int startedSessionsCount,
        Instant startedAt
) {
}
