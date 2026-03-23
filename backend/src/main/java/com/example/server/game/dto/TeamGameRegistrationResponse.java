package com.example.server.game.dto;

import com.example.server.game.entity.GameRegistrationStatus;
import com.example.server.game.entity.GameStatus;

import java.time.Instant;

public record TeamGameRegistrationResponse(
        Long registrationId,
        Long gameId,
        String gameTitle,
        String gameCity,
        GameStatus gameStatus,
        Integer minTeamSize,
        Integer maxTeamSize,
        Instant startsAt,
        GameRegistrationStatus registrationStatus,
        Instant createdAt,
        Instant updatedAt
) {
}
