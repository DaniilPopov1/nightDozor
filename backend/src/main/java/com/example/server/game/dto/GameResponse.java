package com.example.server.game.dto;

import com.example.server.game.entity.GameStatus;

import java.time.Instant;

/**
 * DTO полной информации об игре.
 *
 * @param id идентификатор игры
 * @param title название игры
 * @param description описание игры
 * @param city город игры
 * @param status статус игры
 * @param minTeamSize минимальный размер команды
 * @param maxTeamSize максимальный размер команды
 * @param taskFailurePenaltyMinutes базовый штраф игры
 * @param routeSlotsCount количество маршрутов, которые нужно подготовить для игры
 * @param registrationStartsAt дата начала регистрации
 * @param registrationEndsAt дата окончания регистрации
 * @param startsAt дата старта игры
 * @param finishedAt дата завершения игры
 * @param organizerId идентификатор организатора
 * @param organizerEmail email организатора
 * @param createdAt дата создания игры
 * @param updatedAt дата последнего обновления игры
 */
public record GameResponse(
        Long id,
        String title,
        String description,
        String city,
        GameStatus status,
        Integer minTeamSize,
        Integer maxTeamSize,
        Integer taskFailurePenaltyMinutes,
        Integer routeSlotsCount,
        Instant registrationStartsAt,
        Instant registrationEndsAt,
        Instant startsAt,
        Instant finishedAt,
        Long organizerId,
        String organizerEmail,
        Instant createdAt,
        Instant updatedAt
) {
}
