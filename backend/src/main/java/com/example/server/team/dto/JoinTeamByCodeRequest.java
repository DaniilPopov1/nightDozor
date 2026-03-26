package com.example.server.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса на вступление в команду по invite-коду.
 *
 * @param inviteCode код приглашения команды
 */
public record JoinTeamByCodeRequest(
        @NotBlank @Size(max = 32) String inviteCode
) {
}
