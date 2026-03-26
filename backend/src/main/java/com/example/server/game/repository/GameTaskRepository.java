package com.example.server.game.repository;

import com.example.server.game.entity.GameTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с заданиями игры.
 */
public interface GameTaskRepository extends JpaRepository<GameTask, Long> {
    /**
     * Возвращает задания игры в порядке прохождения.
     *
     * @param gameId идентификатор игры
     * @return список заданий
     */
    List<GameTask> findAllByGameIdOrderByOrderIndexAsc(Long gameId);

    /**
     * Ищет задание по идентификатору в рамках конкретной игры.
     *
     * @param id идентификатор задания
     * @param gameId идентификатор игры
     * @return найденное задание
     */
    Optional<GameTask> findByIdAndGameId(Long id, Long gameId);
}
