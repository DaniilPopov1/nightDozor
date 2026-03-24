package com.example.server.game.dto;

import java.time.Instant;

public record CurrentGameTaskHintResponse(
        Long id,
        Integer orderIndex,
        String text,
        Instant unlockedAt
) {
}
