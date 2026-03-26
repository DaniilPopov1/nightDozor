package com.example.server.game.dto;

import com.example.server.game.entity.GameRegistrationStatus;

import java.time.Instant;

/**
 * DTO заявки команды на участие в игре.
 *
 * @param id идентификатор заявки
 * @param gameId идентификатор игры
 * @param gameTitle название игры
 * @param teamId идентификатор команды
 * @param teamName название команды
 * @param status статус заявки
 * @param createdAt время создания заявки
 * @param updatedAt время обновления заявки
 */
public record GameRegistrationResponse(
        Long id,
        Long gameId,
        String gameTitle,
        Long teamId,
        String teamName,
        GameRegistrationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
