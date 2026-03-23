package com.example.server.game.repository;

import com.example.server.game.entity.TeamGameRouteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeamGameRouteItemRepository extends JpaRepository<TeamGameRouteItem, Long> {
    List<TeamGameRouteItem> findAllByRouteIdOrderByOrderIndexAsc(Long routeId);
}
