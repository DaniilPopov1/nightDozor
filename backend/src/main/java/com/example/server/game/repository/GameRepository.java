package com.example.server.game.repository;

import com.example.server.game.entity.Game;
import com.example.server.game.entity.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Репозиторий для работы с играми.
 */
public interface GameRepository extends JpaRepository<Game, Long> {
    /**
     * Возвращает игры организатора в порядке убывания даты создания.
     *
     * @param organizerId идентификатор организатора
     * @return список игр
     */
    List<Game> findAllByOrganizerIdOrderByCreatedAtDesc(Long organizerId);

    /**
     * Возвращает игры указанного города.
     *
     * @param city город игры
     * @return список игр
     */
    List<Game> findAllByCityIgnoreCaseOrderByCreatedAtDesc(String city);

    /**
     * Возвращает игры в указанном статусе.
     *
     * @param status статус игры
     * @return список игр
     */
    List<Game> findAllByStatusOrderByCreatedAtDesc(GameStatus status);

    /**
     * Возвращает игры в одном из указанных статусов.
     *
     * @param statuses набор статусов
     * @return список игр
     */
    List<Game> findAllByStatusInOrderByCreatedAtDesc(Set<GameStatus> statuses);

    /**
     * Возвращает игры указанного города в одном из статусов.
     *
     * @param city город игры
     * @param statuses набор статусов
     * @return список игр
     */
    List<Game> findAllByCityIgnoreCaseAndStatusInOrderByCreatedAtDesc(String city, Set<GameStatus> statuses);

    /**
     * Ищет игру организатора по идентификатору.
     *
     * @param id идентификатор игры
     * @param organizerId идентификатор организатора
     * @return найденная игра
     */
    Optional<Game> findByIdAndOrganizerId(Long id, Long organizerId);

    /**
     * Ищет игру по идентификатору и статусу.
     *
     * @param id идентификатор игры
     * @param status статус игры
     * @return найденная игра
     */
    Optional<Game> findByIdAndStatus(Long id, GameStatus status);
}
