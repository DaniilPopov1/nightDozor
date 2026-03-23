package com.example.server.game.dto;

import com.example.server.game.entity.GameRegistrationStatus;

import java.time.Instant;

public record IncomingGameRegistrationResponse(
        Long registrationId,
        Long gameId,
        Long teamId,
        String teamName,
        String teamCity,
        Long captainId,
        String captainEmail,
        int activeMembersCount,
        GameRegistrationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
