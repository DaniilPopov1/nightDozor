package com.example.server.game.repository;

import com.example.server.game.entity.TeamGameRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с маршрутами слотов внутри игр.
 */
public interface TeamGameRouteRepository extends JpaRepository<TeamGameRoute, Long> {
    /**
     * Ищет маршрут, уже назначенный конкретной команде в игре.
     *
     * @param gameId идентификатор игры
     * @param teamId идентификатор команды
     * @return найденный маршрут
     */
    Optional<TeamGameRoute> findByGameIdAndAssignedTeamId(Long gameId, Long teamId);

    /**
     * Ищет маршрут по номеру слота в рамках игры.
     *
     * @param gameId идентификатор игры
     * @param slotNumber номер слота маршрута
     * @return найденный маршрут
     */
    Optional<TeamGameRoute> findByGameIdAndSlotNumber(Long gameId, Integer slotNumber);

    /**
     * Ищет маршрут по идентификатору в рамках игры.
     *
     * @param id идентификатор маршрута
     * @param gameId идентификатор игры
     * @return найденный маршрут
     */
    Optional<TeamGameRoute> findByIdAndGameId(Long id, Long gameId);

    /**
     * Возвращает все маршруты игры по номеру слота.
     *
     * @param gameId идентификатор игры
     * @return список маршрутов
     */
    List<TeamGameRoute> findAllByGameIdOrderBySlotNumberAsc(Long gameId);

    /**
     * Возвращает все свободные маршруты игры.
     *
     * @param gameId идентификатор игры
     * @return список свободных маршрутов
     */
    List<TeamGameRoute> findAllByGameIdAndAssignedTeamIsNullOrderBySlotNumberAsc(Long gameId);
}
