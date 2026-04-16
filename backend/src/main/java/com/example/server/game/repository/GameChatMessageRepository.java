package com.example.server.game.repository;

import com.example.server.game.entity.GameChatChannel;
import com.example.server.game.entity.GameChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий сообщений игровых чатов.
 */
public interface GameChatMessageRepository extends JpaRepository<GameChatMessage, Long> {
    /**
     * Возвращает сообщения заданного канала по паре игра-команда.
     *
     * @param gameId идентификатор игры
     * @param teamId идентификатор команды
     * @param channel канал чата
     * @return список сообщений в хронологическом порядке
     */
    List<GameChatMessage> findAllByGameIdAndTeamIdAndChannelOrderByCreatedAtAsc(
            Long gameId,
            Long teamId,
            GameChatChannel channel
    );
}
