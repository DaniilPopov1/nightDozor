package com.example.server.team.dto;

import java.time.Instant;

public record TeamListItemResponse(
        Long id,
        String name,
        String city,
        Long captainId,
        String captainEmail,
        int activeMembersCount,
        Instant createdAt
) {
}
