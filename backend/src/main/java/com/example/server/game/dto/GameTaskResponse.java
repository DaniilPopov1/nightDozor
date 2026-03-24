package com.example.server.game.dto;

import java.time.Instant;
import java.util.List;

public record GameTaskResponse(
        Long id,
        Long gameId,
        String title,
        String riddleText,
        String answerKey,
        Integer orderIndex,
        Integer timeLimitMinutes,
        Integer failurePenaltyMinutes,
        Instant createdAt,
        List<GameTaskHintResponse> hints
) {
}
