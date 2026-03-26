package com.example.server.game.dto;

import com.example.server.game.entity.GameRegistrationStatus;

import java.time.Instant;

/**
 * DTO входящей заявки команды на участие в игре для организатора.
 *
 * @param registrationId идентификатор заявки
 * @param gameId идентификатор игры
 * @param teamId идентификатор команды
 * @param teamName название команды
 * @param teamCity город команды
 * @param captainId идентификатор капитана
 * @param captainEmail email капитана
 * @param activeMembersCount количество активных участников команды
 * @param status статус заявки
 * @param createdAt время создания заявки
 * @param updatedAt время обновления заявки
 */
public record IncomingGameRegistrationResponse(
        Long registrationId,
        Long gameId,
        Long teamId,
        String teamName,
        String teamCity,
        Long captainId,
        String captainEmail,
        int activeMembersCount,
        GameRegistrationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
