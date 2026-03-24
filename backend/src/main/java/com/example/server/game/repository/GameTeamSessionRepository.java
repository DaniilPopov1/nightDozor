package com.example.server.game.repository;

import com.example.server.game.entity.GameTeamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameTeamSessionRepository extends JpaRepository<GameTeamSession, Long> {
    boolean existsByGameId(Long gameId);
    List<GameTeamSession> findAllByGameId(Long gameId);
}
