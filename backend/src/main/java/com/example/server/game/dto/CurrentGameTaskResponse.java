package com.example.server.game.dto;

import com.example.server.game.entity.GameTeamSessionStatus;

import java.time.Instant;
import java.util.List;

/**
 * DTO текущего активного задания команды.
 *
 * @param sessionId идентификатор игровой сессии команды
 * @param gameId идентификатор игры
 * @param gameTitle название игры
 * @param teamId идентификатор команды
 * @param taskId идентификатор задания
 * @param taskTitle название задания
 * @param riddleText текст загадки
 * @param currentOrderIndex текущий номер задания в маршруте
 * @param totalTasks общее число заданий в маршруте
 * @param timeLimitMinutes лимит времени на задание
 * @param failurePenaltyMinutes штраф за провал задания
 * @param totalPenaltyMinutes накопленный штраф команды
 * @param taskStartedAt момент начала текущего задания
 * @param taskDeadlineAt момент дедлайна задания
 * @param remainingSeconds оставшееся время в секундах
 * @param sessionStatus статус игровой сессии
 * @param taskProgressStatus состояние текущего задания
 * @param availableHints список уже открытых подсказок
 */
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
