package com.example.server.team.dto;

import java.time.Instant;
import java.util.List;

public record TeamResponse(
        Long id,
        String name,
        String city,
        String inviteCode,
        Long captainId,
        String captainEmail,
        Instant createdAt,
        List<TeamMemberResponse> members
) {
}
