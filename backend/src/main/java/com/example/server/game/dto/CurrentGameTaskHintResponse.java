package com.example.server.game.dto;

import java.time.Instant;

/**
 * DTO доступной подсказки текущего задания.
 *
 * @param id идентификатор подсказки
 * @param orderIndex порядок подсказки
 * @param text текст подсказки
 * @param unlockedAt момент открытия подсказки
 */
public record CurrentGameTaskHintResponse(
        Long id,
        Integer orderIndex,
        String text,
        Instant unlockedAt
) {
}
