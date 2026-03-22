package com.example.server.game.dto;

import com.example.server.game.entity.GameStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateGameStatusRequest(
        @NotNull GameStatus status
) {
}
