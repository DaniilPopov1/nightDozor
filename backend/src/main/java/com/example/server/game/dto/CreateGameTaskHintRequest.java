package com.example.server.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateGameTaskHintRequest(
        @NotBlank @Size(max = 4000) String text,
        @NotNull @Min(1) @Max(2) Integer orderIndex,
        @NotNull @Min(0) Integer delayMinutesFromPreviousHint
) {
}
