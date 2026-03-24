package com.example.server.game.repository;

import com.example.server.game.entity.GameTeamSession;
import com.example.server.game.entity.GameTeamSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameTeamSessionRepository extends JpaRepository<GameTeamSession, Long> {
    boolean existsByGameId(Long gameId);
    List<GameTeamSession> findAllByGameId(Long gameId);
    Optional<GameTeamSession> findByTeamIdAndStatus(Long teamId, GameTeamSessionStatus status);
}
