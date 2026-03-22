package com.example.server.game.dto;

import com.example.server.game.entity.GameStatus;

import java.time.Instant;

public record GameResponse(
        Long id,
        String title,
        String description,
        String city,
        GameStatus status,
        Integer minTeamSize,
        Integer maxTeamSize,
        Integer taskFailurePenaltyMinutes,
        Instant registrationStartsAt,
        Instant registrationEndsAt,
        Instant startsAt,
        Instant finishedAt,
        Long organizerId,
        String organizerEmail,
        Instant createdAt,
        Instant updatedAt
) {
}
