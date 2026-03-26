package com.example.server.game.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO запроса на добавление задания в маршрут команды.
 *
 * @param taskId идентификатор задания
 * @param orderIndex порядковый номер задания в маршруте
 */
public record AddTaskToRouteRequest(
        @NotNull Long taskId,
        @NotNull @Min(1) Integer orderIndex
) {
}
