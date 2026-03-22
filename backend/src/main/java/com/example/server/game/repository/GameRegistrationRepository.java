package com.example.server.game.repository;

import com.example.server.game.entity.GameRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameRegistrationRepository extends JpaRepository<GameRegistration, Long> {
    Optional<GameRegistration> findByGameIdAndTeamId(Long gameId, Long teamId);
}
