package com.example.server.game.dto;

/**
 * DTO элемента маршрута команды.
 *
 * @param id идентификатор элемента маршрута
 * @param orderIndex порядок задания в маршруте
 * @param taskId идентификатор задания
 * @param taskTitle название задания
 */
public record TeamGameRouteItemResponse(
        Long id,
        Integer orderIndex,
        Long taskId,
        String taskTitle
) {
}
