package com.example.server.game.dto;

import com.example.server.game.entity.GameChatChannel;

import java.time.Instant;

/**
 * DTO сообщения игрового чата.
 *
 * @param id идентификатор сообщения
 * @param gameId идентификатор игры
 * @param teamId идентификатор команды
 * @param channel канал сообщения
 * @param senderId идентификатор отправителя
 * @param senderEmail email отправителя
 * @param text текст сообщения
 * @param createdAt время отправки
 */
public record GameChatMessageResponse(
        Long id,
        Long gameId,
        Long teamId,
        GameChatChannel channel,
        Long senderId,
        String senderEmail,
        String text,
        Instant createdAt
) {
}
