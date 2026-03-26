package com.example.server.game.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO задания игры.
 *
 * @param id идентификатор задания
 * @param gameId идентификатор игры
 * @param title название задания
 * @param riddleText текст загадки
 * @param answerKey правильный ключ
 * @param orderIndex порядок задания
 * @param timeLimitMinutes лимит времени
 * @param failurePenaltyMinutes штраф за провал
 * @param createdAt дата создания задания
 * @param hints список подсказок задания
 */
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
