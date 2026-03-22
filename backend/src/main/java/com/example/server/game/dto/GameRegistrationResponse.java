package com.example.server.game.dto;

import com.example.server.game.entity.GameRegistrationStatus;

import java.time.Instant;

public record GameRegistrationResponse(
        Long id,
        Long gameId,
        String gameTitle,
        Long teamId,
        String teamName,
        GameRegistrationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
