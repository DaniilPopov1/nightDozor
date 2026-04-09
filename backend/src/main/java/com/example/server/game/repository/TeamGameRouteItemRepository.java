package com.example.server.game.repository;

import com.example.server.game.entity.TeamGameRouteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий для работы с элементами маршрутов команд.
 */
public interface TeamGameRouteItemRepository extends JpaRepository<TeamGameRouteItem, Long> {
    /**
     * Возвращает элементы маршрута в порядке прохождения.
     *
     * @param routeId идентификатор маршрута
     * @return список элементов маршрута
     */
    List<TeamGameRouteItem> findAllByRouteIdOrderByOrderIndexAsc(Long routeId);

    /**
     * Проверяет, используется ли задание хотя бы в одном маршруте конкретной игры.
     *
     * @param taskId идентификатор задания
     * @param gameId идентификатор игры
     * @return {@code true}, если задание уже включено в один из маршрутов игры
     */
    boolean existsByTaskIdAndRouteGameId(Long taskId, Long gameId);
}
