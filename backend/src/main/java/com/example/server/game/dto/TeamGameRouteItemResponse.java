package com.example.server.game.dto;

public record TeamGameRouteItemResponse(
        Long id,
        Integer orderIndex,
        Long taskId,
        String taskTitle
) {
}
