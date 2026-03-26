package com.example.server.game.dto;

/**
 * DTO подсказки задания игры.
 *
 * @param id идентификатор подсказки
 * @param orderIndex порядок подсказки
 * @param text текст подсказки
 * @param delayMinutesFromPreviousHint задержка показа
 */
public record GameTaskHintResponse(
        Long id,
        Integer orderIndex,
        String text,
        Integer delayMinutesFromPreviousHint
) {
}
