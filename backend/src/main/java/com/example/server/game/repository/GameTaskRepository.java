package com.example.server.game.repository;

import com.example.server.game.entity.GameTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameTaskRepository extends JpaRepository<GameTask, Long> {
    List<GameTask> findAllByGameIdOrderByOrderIndexAsc(Long gameId);
}
