package com.example.server.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTeamGameRouteRequest(
        @NotNull Long teamId,
        @NotBlank @Size(max = 150) String name
) {
}
