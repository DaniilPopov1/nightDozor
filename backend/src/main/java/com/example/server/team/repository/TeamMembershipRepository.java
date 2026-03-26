package com.example.server.team.repository;

import com.example.server.team.entity.TeamMembership;
import com.example.server.team.entity.TeamMembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с членством пользователей в командах.
 */
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {
    /**
     * Возвращает все записи членства для команды.
     *
     * @param teamId идентификатор команды
     * @return список членств команды
     */
    List<TeamMembership> findAllByTeamId(Long teamId);

    /**
     * Возвращает членства команды в указанном статусе.
     *
     * @param teamId идентификатор команды
     * @param status статус членства
     * @return список членств
     */
    List<TeamMembership> findAllByTeamIdAndStatus(Long teamId, TeamMembershipStatus status);

    /**
     * Ищет активное или иное членство пользователя по статусу.
     *
     * @param userId идентификатор пользователя
     * @param status статус членства
     * @return найденное членство
     */
    Optional<TeamMembership> findByUserIdAndStatus(Long userId, TeamMembershipStatus status);

    /**
     * Возвращает все членства пользователя с указанным статусом.
     *
     * @param userId идентификатор пользователя
     * @param status статус членства
     * @return список членств пользователя
     */
    List<TeamMembership> findAllByUserIdAndStatus(Long userId, TeamMembershipStatus status);

    /**
     * Ищет членство конкретного пользователя в конкретной команде.
     *
     * @param teamId идентификатор команды
     * @param userId идентификатор пользователя
     * @return найденное членство
     */
    Optional<TeamMembership> findByTeamIdAndUserId(Long teamId, Long userId);

    /**
     * Считает количество участников команды в указанном статусе.
     *
     * @param teamId идентификатор команды
     * @param status статус членства
     * @return количество записей
     */
    long countByTeamIdAndStatus(Long teamId, TeamMembershipStatus status);

    /**
     * Проверяет наличие у пользователя членства в указанном статусе.
     *
     * @param userId идентификатор пользователя
     * @param status статус членства
     * @return {@code true}, если членство найдено
     */
    boolean existsByUserIdAndStatus(Long userId, TeamMembershipStatus status);
}
