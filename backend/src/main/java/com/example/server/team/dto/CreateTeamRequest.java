package com.example.server.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO запроса на создание команды.
 *
 * @param name название команды
 * @param city город команды
 */
public record CreateTeamRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String city
) {
}
