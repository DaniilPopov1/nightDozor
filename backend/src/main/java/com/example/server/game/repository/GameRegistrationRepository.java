package com.example.server.game.repository;

import com.example.server.game.entity.GameRegistration;
import com.example.server.game.entity.GameRegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameRegistrationRepository extends JpaRepository<GameRegistration, Long> {
    Optional<GameRegistration> findByGameIdAndTeamId(Long gameId, Long teamId);
    List<GameRegistration> findAllByGameIdAndStatusOrderByCreatedAtDesc(Long gameId, GameRegistrationStatus status);
    List<GameRegistration> findAllByTeamIdOrderByCreatedAtDesc(Long teamId);
}
