package com.example.server.game.dto;

import com.example.server.game.entity.GameTeamSessionStatus;

import java.time.Instant;

/**
 * DTO строки общего зачета команды в игре.
 *
 * @param place место команды
 * @param teamId идентификатор команды
 * @param teamName название команды
 * @param completedTasksCount число завершенных заданий
 * @param totalTasksCount общее число заданий
 * @param totalPenaltyMinutes накопленный штраф
 * @param elapsedSeconds затраченное время
 * @param totalScoreSeconds итоговый score
 * @param sessionStatus статус игровой сессии
 * @param finishedAt время завершения сессии
 */
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
