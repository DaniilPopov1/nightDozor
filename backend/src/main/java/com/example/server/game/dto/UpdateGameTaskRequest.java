package com.example.server.game.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса на обновление задания игры.
 *
 * @param title название задания
 * @param riddleText текст загадки
 * @param answerKey правильный ключ
 * @param orderIndex порядок задания
 * @param timeLimitMinutes лимит времени
 * @param failurePenaltyMinutes штраф за провал
 */
public record UpdateGameTaskRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 4000) String riddleText,
        @NotBlank @Size(max = 255) String answerKey,
        @NotNull @Min(1) Integer orderIndex,
        @NotNull @Min(1) Integer timeLimitMinutes,
        @NotNull @Min(0) Integer failurePenaltyMinutes
) {
}
