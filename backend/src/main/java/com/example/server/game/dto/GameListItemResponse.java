package com.example.server.game.dto;

import com.example.server.game.entity.GameStatus;

import java.time.Instant;

public record GameListItemResponse(
        Long id,
        String title,
        String city,
        GameStatus status,
        Integer minTeamSize,
        Integer maxTeamSize,
        Instant registrationStartsAt,
        Instant registrationEndsAt,
        Instant startsAt,
        Instant createdAt
) {
}
