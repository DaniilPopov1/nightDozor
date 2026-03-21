package com.example.server.team.dto;

import com.example.server.team.entity.TeamMembershipStatus;

import java.time.Instant;

public record IncomingJoinRequestResponse(
        Long teamId,
        Long userId,
        String userEmail,
        TeamMembershipStatus status,
        Instant createdAt
) {
}
