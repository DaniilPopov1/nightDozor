package com.example.server.team.dto;

import com.example.server.team.entity.TeamMembershipStatus;

import java.time.Instant;

public record TeamJoinRequestDecisionResponse(
        Long teamId,
        Long userId,
        String userEmail,
        TeamMembershipStatus status,
        Instant joinedAt,
        Instant updatedAt
) {
}
