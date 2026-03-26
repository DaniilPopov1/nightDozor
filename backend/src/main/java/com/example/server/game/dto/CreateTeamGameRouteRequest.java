package com.example.server.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса на создание маршрута заданий для команды.
 *
 * @param teamId идентификатор команды
 * @param name название маршрута
 */
public record CreateTeamGameRouteRequest(
        @NotNull Long teamId,
        @NotBlank @Size(max = 150) String name
) {
}
