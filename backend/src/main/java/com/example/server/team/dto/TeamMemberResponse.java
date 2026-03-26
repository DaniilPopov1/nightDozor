package com.example.server.team.dto;

import com.example.server.team.entity.TeamMembershipRole;
import com.example.server.team.entity.TeamMembershipStatus;

import java.time.Instant;

/**
 * DTO участника команды.
 *
 * @param userId идентификатор пользователя
 * @param email email пользователя
 * @param role роль в команде
 * @param status статус членства
 * @param joinedAt время вступления в команду
 */
public record TeamMemberResponse(
        Long userId,
        String email,
        TeamMembershipRole role,
        TeamMembershipStatus status,
        Instant joinedAt
) {
}
