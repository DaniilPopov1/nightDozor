package com.example.server.team.dto;

import com.example.server.team.entity.TeamMembershipStatus;

import java.time.Instant;

/**
 * DTO входящей заявки на вступление в команду для капитана.
 *
 * @param teamId идентификатор команды
 * @param userId идентификатор пользователя-заявителя
 * @param userEmail email пользователя-заявителя
 * @param status статус заявки
 * @param createdAt время создания заявки
 */
public record IncomingJoinRequestResponse(
        Long teamId,
        Long userId,
        String userEmail,
        TeamMembershipStatus status,
        Instant createdAt
) {
}
