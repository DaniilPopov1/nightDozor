package com.example.server.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinTeamByCodeRequest(
        @NotBlank @Size(max = 32) String inviteCode
) {
}
