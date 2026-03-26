package com.example.server.game.repository;

import com.example.server.game.entity.GameTeamSession;
import com.example.server.game.entity.GameTeamSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с игровыми сессиями команд.
 */
public interface GameTeamSessionRepository extends JpaRepository<GameTeamSession, Long> {
    /**
     * Проверяет, существуют ли сессии для указанной игры.
     *
     * @param gameId идентификатор игры
     * @return {@code true}, если сессии уже созданы
     */
    boolean existsByGameId(Long gameId);

    /**
     * Проверяет, существуют ли сессии игры в указанном статусе.
     *
     * @param gameId идентификатор игры
     * @param status статус сессии
     * @return {@code true}, если такие сессии существуют
     */
    boolean existsByGameIdAndStatus(Long gameId, GameTeamSessionStatus status);

    /**
     * Возвращает все игровые сессии указанной игры.
     *
     * @param gameId идентификатор игры
     * @return список игровых сессий
     */
    List<GameTeamSession> findAllByGameId(Long gameId);

    /**
     * Ищет игровую сессию команды в указанном статусе.
     *
     * @param teamId идентификатор команды
     * @param status статус сессии
     * @return найденная сессия
     */
    Optional<GameTeamSession> findByTeamIdAndStatus(Long teamId, GameTeamSessionStatus status);

    /**
     * Возвращает последнюю игровую сессию команды.
     *
     * @param teamId идентификатор команды
     * @return последняя сессия команды
     */
    Optional<GameTeamSession> findTopByTeamIdOrderByStartedAtDesc(Long teamId);
}
