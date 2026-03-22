package com.example.server.game.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateGameRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotBlank @Size(max = 120) String city,
        @NotNull @Min(1) Integer minTeamSize,
        @NotNull @Min(1) Integer maxTeamSize,
        @NotNull @Min(0) Integer taskFailurePenaltyMinutes,
        Instant registrationStartsAt,
        Instant registrationEndsAt,
        @NotNull Instant startsAt
) {
}
