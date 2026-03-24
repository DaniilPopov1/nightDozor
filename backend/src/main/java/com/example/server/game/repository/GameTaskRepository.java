package com.example.server.game.repository;

import com.example.server.game.entity.GameTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameTaskRepository extends JpaRepository<GameTask, Long> {
    List<GameTask> findAllByGameIdOrderByOrderIndexAsc(Long gameId);
    Optional<GameTask> findByIdAndGameId(Long id, Long gameId);
}
