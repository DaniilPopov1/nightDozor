package com.example.server.game.dto;

import com.example.server.game.entity.GameTeamSessionStatus;

import java.time.Instant;
import java.util.List;

public record GameTeamProgressResponse(
        Long gameId,
        String gameTitle,
        Long teamId,
        String teamName,
        int currentPlace,
        int completedTasksCount,
        int totalTasksCount,
        Integer totalPenaltyMinutes,
        long elapsedSeconds,
        long totalScoreSeconds,
        GameTeamSessionStatus sessionStatus,
        Instant startedAt,
        Instant finishedAt,
        List<GameTeamStandingResponse> standings
) {
}
