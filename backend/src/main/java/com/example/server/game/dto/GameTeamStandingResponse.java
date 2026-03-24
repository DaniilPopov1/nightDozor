package com.example.server.game.dto;

import com.example.server.game.entity.GameTeamSessionStatus;

import java.time.Instant;

public record GameTeamStandingResponse(
        int place,
        Long teamId,
        String teamName,
        int completedTasksCount,
        int totalTasksCount,
        Integer totalPenaltyMinutes,
        long elapsedSeconds,
        long totalScoreSeconds,
        GameTeamSessionStatus sessionStatus,
        Instant finishedAt
) {
}
