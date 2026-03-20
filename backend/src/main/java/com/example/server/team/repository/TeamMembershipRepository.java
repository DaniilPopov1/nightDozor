package com.example.server.team.repository;

import com.example.server.team.entity.TeamMembership;
import com.example.server.team.entity.TeamMembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {
    List<TeamMembership> findAllByTeamId(Long teamId);
    List<TeamMembership> findAllByTeamIdAndStatus(Long teamId, TeamMembershipStatus status);
    Optional<TeamMembership> findByUserIdAndStatus(Long userId, TeamMembershipStatus status);
    Optional<TeamMembership> findByTeamIdAndUserId(Long teamId, Long userId);
    boolean existsByUserIdAndStatus(Long userId, TeamMembershipStatus status);
}
