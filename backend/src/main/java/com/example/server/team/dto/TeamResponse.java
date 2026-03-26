package com.example.server.team.dto;

import java.time.Instant;
import java.util.List;

/**
 * DTO полной информации о команде.
 *
 * @param id идентификатор команды
 * @param name название команды
 * @param city город команды
 * @param inviteCode invite-код для вступления
 * @param captainId идентификатор капитана
 * @param captainEmail email капитана
 * @param createdAt дата создания команды
 * @param members список активных участников команды
 */
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
