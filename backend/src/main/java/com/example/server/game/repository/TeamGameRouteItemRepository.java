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
}
