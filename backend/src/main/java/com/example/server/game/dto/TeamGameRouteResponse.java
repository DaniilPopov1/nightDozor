package com.example.server.game.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO маршрута команды в игре.
 *
 * @param id идентификатор маршрута
 * @param gameId идентификатор игры
 * @param teamId идентификатор команды
 * @param teamName название команды
 * @param name название маршрута
 * @param createdAt дата создания маршрута
 * @param items список заданий маршрута
 */
public record TeamGameRouteResponse(
        Long id,
        Long gameId,
        Long teamId,
        String teamName,
        String name,
        Instant createdAt,
        List<TeamGameRouteItemResponse> items
) {
}
