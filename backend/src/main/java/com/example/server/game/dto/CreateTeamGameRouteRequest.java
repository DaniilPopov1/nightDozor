package com.example.server.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса на создание заранее подготовленного маршрута игры.
 *
 * @param slotNumber номер маршрута в игре
 * @param name название маршрута
 */
public record CreateTeamGameRouteRequest(
        @NotNull Long slotNumber,
        @NotBlank @Size(max = 150) String name
) {
}
