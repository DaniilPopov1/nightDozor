package com.example.server.game.dto;

import java.time.Instant;

public record GameStartResponse(
        Long gameId,
        String gameTitle,
        int startedSessionsCount,
        Instant startedAt
) {
}
