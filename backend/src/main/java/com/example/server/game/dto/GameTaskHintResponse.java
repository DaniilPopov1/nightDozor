package com.example.server.game.dto;

public record GameTaskHintResponse(
        Long id,
        Integer orderIndex,
        String text,
        Integer delayMinutesFromPreviousHint
) {
}
