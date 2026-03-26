package com.example.server.team.dto;

import com.example.server.team.entity.TeamMembershipStatus;

import java.time.Instant;

/**
 * DTO исходящей заявки пользователя на вступление в команду.
 *
 * @param teamId идентификатор команды
 * @param teamName название команды
 * @param city город команды
 * @param status статус заявки
 * @param createdAt время создания заявки
 * @param updatedAt время последнего обновления заявки
 */
public record OutgoingJoinRequestResponse(
        Long teamId,
        String teamName,
        String city,
        TeamMembershipStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
