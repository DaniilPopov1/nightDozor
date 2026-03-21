package com.example.server.team.dto;

import com.example.server.team.entity.TeamMembershipStatus;

import java.time.Instant;

public record OutgoingJoinRequestResponse(
        Long teamId,
        String teamName,
        String city,
        TeamMembershipStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
