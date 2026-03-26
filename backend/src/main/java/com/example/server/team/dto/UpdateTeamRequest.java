package com.example.server.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса на обновление данных команды.
 *
 * @param name новое название команды
 * @param city новый город команды
 * @param regenerateInviteCode признак регенерации invite-кода
 */
public record UpdateTeamRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String city,
        boolean regenerateInviteCode
) {
}
