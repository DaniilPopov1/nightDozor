package com.example.server.game.dto;

import com.example.server.game.entity.GameTeamSessionStatus;

import java.time.Instant;

/**
 * DTO результата отправки ключа задания.
 *
 * @param sessionId идентификатор игровой сессии
 * @param teamId идентификатор команды
 * @param completedTaskId идентификатор завершенного задания
 * @param completedTaskTitle название завершенного задания
 * @param completedOrderIndex номер завершенного задания в маршруте
 * @param totalPenaltyMinutes накопленный штраф команды
 * @param gameSessionFinished признак завершения игровой сессии
 * @param sessionStatus статус игровой сессии
 * @param nextTaskId идентификатор следующего задания
 * @param nextTaskTitle название следующего задания
 * @param nextOrderIndex номер следующего задания
 * @param submittedAt время отправки ключа
 */
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
