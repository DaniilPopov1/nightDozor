package com.example.server.game.repository;

import com.example.server.game.entity.TeamGameRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamGameRouteRepository extends JpaRepository<TeamGameRoute, Long> {
    Optional<TeamGameRoute> findByGameIdAndTeamId(Long gameId, Long teamId);
    List<TeamGameRoute> findAllByGameIdOrderByCreatedAtAsc(Long gameId);
}
