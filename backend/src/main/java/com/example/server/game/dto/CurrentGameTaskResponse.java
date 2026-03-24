package com.example.server.game.dto;

import com.example.server.game.entity.GameTeamSessionStatus;

import java.time.Instant;
import java.util.List;

public record CurrentGameTaskResponse(
        Long sessionId,
        Long gameId,
        String gameTitle,
        Long teamId,
        Long taskId,
        String taskTitle,
        String riddleText,
        Integer currentOrderIndex,
        int totalTasks,
        Integer timeLimitMinutes,
        Integer failurePenaltyMinutes,
        Integer totalPenaltyMinutes,
        Instant taskStartedAt,
        Instant taskDeadlineAt,
        long remainingSeconds,
        GameTeamSessionStatus sessionStatus,
        String taskProgressStatus,
        List<CurrentGameTaskHintResponse> availableHints
) {
}
