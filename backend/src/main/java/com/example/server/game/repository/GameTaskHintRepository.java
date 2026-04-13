package com.example.server.game.repository;

import com.example.server.game.entity.GameTaskHint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий для работы с подсказками заданий.
 */
public interface GameTaskHintRepository extends JpaRepository<GameTaskHint, Long> {
    /**
     * Возвращает подсказки задания в порядке показа.
     *
     * @param taskId идентификатор задания
     * @return список подсказок
     */
    List<GameTaskHint> findAllByTaskIdOrderByOrderIndexAsc(Long taskId);

    java.util.Optional<GameTaskHint> findByIdAndTaskId(Long id, Long taskId);
}
