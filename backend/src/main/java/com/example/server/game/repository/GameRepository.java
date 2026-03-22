package com.example.server.game.repository;

import com.example.server.game.entity.Game;
import com.example.server.game.entity.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findAllByOrganizerIdOrderByCreatedAtDesc(Long organizerId);
    List<Game> findAllByCityIgnoreCaseOrderByCreatedAtDesc(String city);
    List<Game> findAllByStatusOrderByCreatedAtDesc(GameStatus status);
    List<Game> findAllByStatusInOrderByCreatedAtDesc(Set<GameStatus> statuses);
    List<Game> findAllByCityIgnoreCaseAndStatusInOrderByCreatedAtDesc(String city, Set<GameStatus> statuses);
    Optional<Game> findByIdAndOrganizerId(Long id, Long organizerId);
}
