package com.example.server.game.repository;

import com.example.server.game.entity.GameTaskHint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameTaskHintRepository extends JpaRepository<GameTaskHint, Long> {
    List<GameTaskHint> findAllByTaskIdOrderByOrderIndexAsc(Long taskId);
}
