package com.example.server.game.repository;

import com.example.server.game.entity.GameRegistration;
import com.example.server.game.entity.GameRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с заявками команд на участие в играх.
 */
public interface GameRegistrationRepository extends JpaRepository<GameRegistration, Long> {
    /**
     * Ищет заявку команды на участие в конкретной игре.
     *
     * @param gameId идентификатор игры
     * @param teamId идентификатор команды
     * @return найденная заявка
     */
    Optional<GameRegistration> findByGameIdAndTeamId(Long gameId, Long teamId);

    /**
     * Ищет заявку по идентификатору в рамках конкретной игры.
     *
     * @param id идентификатор заявки
     * @param gameId идентификатор игры
     * @return найденная заявка
     */
    Optional<GameRegistration> findByIdAndGameId(Long id, Long gameId);

    /**
     * Возвращает заявки игры в указанном статусе.
     *
     * @param gameId идентификатор игры
     * @param status статус заявки
     * @return список заявок
     */
    List<GameRegistration> findAllByGameIdAndStatusOrderByCreatedAtDesc(Long gameId, GameRegistrationStatus status);

    /**
     * Возвращает все заявки команды на игры.
     *
     * @param teamId идентификатор команды
     * @return список заявок команды
     */
    List<GameRegistration> findAllByTeamIdOrderByCreatedAtDesc(Long teamId);
}
