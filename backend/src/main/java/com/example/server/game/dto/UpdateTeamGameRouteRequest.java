package com.example.server.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса на обновление маршрута игры.
 *
 * @param name название маршрута
 */
public record UpdateTeamGameRouteRequest(
        @NotBlank @Size(max = 150) String name
) {
}
