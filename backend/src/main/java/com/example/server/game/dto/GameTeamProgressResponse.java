package com.example.server.game.dto;

import com.example.server.game.entity.GameTeamSessionStatus;

import java.time.Instant;
import java.util.List;

/**
 * DTO прогресса текущей команды в игре.
 *
 * @param gameId идентификатор игры
 * @param gameTitle название игры
 * @param teamId идентификатор команды
 * @param teamName название команды
 * @param currentPlace текущее место команды
 * @param completedTasksCount число завершенных заданий
 * @param totalTasksCount общее число заданий
 * @param totalPenaltyMinutes накопленный штраф
 * @param elapsedSeconds затраченное время
 * @param totalScoreSeconds итоговый score
 * @param sessionStatus статус игровой сессии
 * @param startedAt время старта сессии
 * @param finishedAt время завершения сессии
 * @param standings общий зачет игры
 */
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
