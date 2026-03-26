package com.example.server.team.dto;

import com.example.server.team.entity.TeamMembershipStatus;

import java.time.Instant;

/**
 * DTO результата подтверждения или отклонения заявки на вступление в команду.
 *
 * @param teamId идентификатор команды
 * @param userId идентификатор пользователя
 * @param userEmail email пользователя
 * @param status итоговый статус заявки
 * @param joinedAt время фактического вступления в команду
 * @param updatedAt время обновления записи
 */
public record TeamJoinRequestDecisionResponse(
        Long teamId,
        Long userId,
        String userEmail,
        TeamMembershipStatus status,
        Instant joinedAt,
        Instant updatedAt
) {
}
