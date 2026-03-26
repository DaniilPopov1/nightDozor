package com.example.server.game.dto;

import com.example.server.game.entity.GameStatus;

import java.time.Instant;

/**
 * DTO компактного представления игры в списке.
 *
 * @param id идентификатор игры
 * @param title название игры
 * @param city город игры
 * @param status статус игры
 * @param minTeamSize минимальный размер команды
 * @param maxTeamSize максимальный размер команды
 * @param registrationStartsAt дата начала регистрации
 * @param registrationEndsAt дата окончания регистрации
 * @param startsAt дата старта игры
 * @param createdAt дата создания игры
 */
public record GameListItemResponse(
        Long id,
        String title,
        String city,
        GameStatus status,
        Integer minTeamSize,
        Integer maxTeamSize,
        Instant registrationStartsAt,
        Instant registrationEndsAt,
        Instant startsAt,
        Instant createdAt
) {
}
