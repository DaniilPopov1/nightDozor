package com.example.server.team.dto;

import com.example.server.team.entity.TeamMembershipRole;
import com.example.server.team.entity.TeamMembershipStatus;

import java.time.Instant;

public record TeamMemberResponse(
        Long userId,
        String email,
        TeamMembershipRole role,
        TeamMembershipStatus status,
        Instant joinedAt
) {
}
