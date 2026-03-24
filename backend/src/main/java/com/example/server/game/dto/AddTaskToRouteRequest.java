package com.example.server.game.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddTaskToRouteRequest(
        @NotNull Long taskId,
        @NotNull @Min(1) Integer orderIndex
) {
}
