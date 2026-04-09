package com.example.server.game.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO маршрута слота в игре.
 *
 * @param id идентификатор маршрута
 * @param gameId идентификатор игры
 * @param slotNumber номер маршрута в игре
 * @param assignedTeamId идентификатор команды, которой назначен маршрут
 * @param assignedTeamName название команды, которой назначен маршрут
 * @param name название маршрута
 * @param createdAt дата создания маршрута
 * @param items список заданий маршрута
 */
public record TeamGameRouteResponse(
        Long id,
        Long gameId,
        Integer slotNumber,
        Long assignedTeamId,
        String assignedTeamName,
        String name,
        Instant createdAt,
        List<TeamGameRouteItemResponse> items
) {
}
