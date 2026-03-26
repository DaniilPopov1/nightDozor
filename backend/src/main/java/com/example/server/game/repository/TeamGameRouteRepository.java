package com.example.server.game.repository;

import com.example.server.game.entity.TeamGameRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с маршрутами команд внутри игр.
 */
public interface TeamGameRouteRepository extends JpaRepository<TeamGameRoute, Long> {
    /**
     * Ищет маршрут команды в конкретной игре.
     *
     * @param gameId идентификатор игры
     * @param teamId идентификатор команды
     * @return найденный маршрут
     */
    Optional<TeamGameRoute> findByGameIdAndTeamId(Long gameId, Long teamId);

    /**
     * Ищет маршрут по идентификатору в рамках игры.
     *
     * @param id идентификатор маршрута
     * @param gameId идентификатор игры
     * @return найденный маршрут
     */
    Optional<TeamGameRoute> findByIdAndGameId(Long id, Long gameId);

    /**
     * Возвращает все маршруты игры в порядке создания.
     *
     * @param gameId идентификатор игры
     * @return список маршрутов
     */
    List<TeamGameRoute> findAllByGameIdOrderByCreatedAtAsc(Long gameId);
}
