package com.example.server.game.dto;

import com.example.server.game.entity.GameRegistrationStatus;
import com.example.server.game.entity.GameStatus;

import java.time.Instant;

/**
 * DTO заявки текущей команды на участие в игре.
 *
 * @param registrationId идентификатор заявки
 * @param gameId идентификатор игры
 * @param gameTitle название игры
 * @param gameCity город игры
 * @param gameStatus статус игры
 * @param minTeamSize минимальный размер команды
 * @param maxTeamSize максимальный размер команды
 * @param startsAt дата старта игры
 * @param registrationStatus статус заявки
 * @param createdAt время создания заявки
 * @param updatedAt время обновления заявки
 */
public record TeamGameRegistrationResponse(
        Long registrationId,
        Long gameId,
        String gameTitle,
        String gameCity,
        GameStatus gameStatus,
        Integer minTeamSize,
        Integer maxTeamSize,
        Instant startsAt,
        GameRegistrationStatus registrationStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
