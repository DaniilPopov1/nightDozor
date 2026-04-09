package com.example.server.game.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTO запроса на создание игры.
 *
 * @param title название игры
 * @param description описание игры
 * @param city город проведения игры
 * @param minTeamSize минимальный размер команды
 * @param maxTeamSize максимальный размер команды
 * @param taskFailurePenaltyMinutes базовый штраф игры
 * @param routeSlotsCount количество заранее подготавливаемых маршрутов
 * @param registrationStartsAt дата начала регистрации
 * @param registrationEndsAt дата окончания регистрации
 * @param startsAt дата и время старта игры
 */
public record CreateGameRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotBlank @Size(max = 120) String city,
        @NotNull @Min(1) Integer minTeamSize,
        @NotNull @Min(1) Integer maxTeamSize,
        @NotNull @Min(0) Integer taskFailurePenaltyMinutes,
        @NotNull @Min(1) Integer routeSlotsCount,
        Instant registrationStartsAt,
        Instant registrationEndsAt,
        @NotNull Instant startsAt
) {
}
