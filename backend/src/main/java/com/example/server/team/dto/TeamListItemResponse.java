package com.example.server.team.dto;

import java.time.Instant;

/**
 * DTO компактного представления команды в списке.
 *
 * @param id идентификатор команды
 * @param name название команды
 * @param city город команды
 * @param captainId идентификатор капитана
 * @param captainEmail email капитана
 * @param activeMembersCount количество активных участников
 * @param createdAt дата создания команды
 */
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
