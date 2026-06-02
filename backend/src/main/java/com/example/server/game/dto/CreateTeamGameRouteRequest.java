package com.example.server.game.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO запроса на создание заранее подготовленного маршрута игры.
 *
 * @param slotNumber номер маршрута в игре
 */
public record CreateTeamGameRouteRequest(
        @NotNull Long slotNumber
) {
}
