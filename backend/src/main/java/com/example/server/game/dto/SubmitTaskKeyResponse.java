package com.example.server.game.dto;

import com.example.server.game.entity.GameTeamSessionStatus;

import java.time.Instant;

public record SubmitTaskKeyResponse(
        Long sessionId,
        Long teamId,
        Long completedTaskId,
        String completedTaskTitle,
        Integer completedOrderIndex,
        Integer totalPenaltyMinutes,
        boolean gameSessionFinished,
        GameTeamSessionStatus sessionStatus,
        Long nextTaskId,
        String nextTaskTitle,
        Integer nextOrderIndex,
        Instant submittedAt
) {
}
