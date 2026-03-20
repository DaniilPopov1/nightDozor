package com.example.server.team.service;

import com.example.server.auth.entity.Role;
import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import com.example.server.common.exception.BadRequestException;
import com.example.server.common.exception.ConflictException;
import com.example.server.common.exception.NotFoundException;
import com.example.server.team.dto.CreateTeamRequest;
import com.example.server.team.dto.JoinTeamByCodeRequest;
import com.example.server.team.dto.TeamMemberResponse;
import com.example.server.team.dto.TeamResponse;
import com.example.server.team.entity.Team;
import com.example.server.team.entity.TeamMembership;
import com.example.server.team.entity.TeamMembershipRole;
import com.example.server.team.entity.TeamMembershipStatus;
import com.example.server.team.repository.TeamMembershipRepository;
import com.example.server.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TeamService {

    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 8;

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamResponse createTeam(String email, CreateTeamRequest request) {
        User user = getUserByEmail(email);
        validateCanCreateTeam(user);

        Team team = new Team();
        team.setName(request.name().trim());
        team.setCity(request.city().trim());
        team.setCaptain(user);
        team.setInviteCode(generateUniqueInviteCode());

        Team savedTeam = teamRepository.save(team);

        TeamMembership membership = new TeamMembership();
        membership.setTeam(savedTeam);
        membership.setUser(user);
        membership.setRole(TeamMembershipRole.CAPTAIN);
        membership.setStatus(TeamMembershipStatus.ACTIVE);
        membership.setJoinedAt(Instant.now());

        teamMembershipRepository.save(membership);

        return buildTeamResponse(savedTeam);
    }

    @Transactional
    public TeamResponse joinTeamByCode(String email, JoinTeamByCodeRequest request) {
        User user = getUserByEmail(email);
        validateCanJoinTeam(user);

        String inviteCode = normalizeInviteCode(request.inviteCode());
        Team team = teamRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new NotFoundException("Команда с таким кодом приглашения не найдена"));

        if (team.getCaptain().getId().equals(user.getId())) {
            throw new ConflictException("Капитан уже состоит в этой команде");
        }

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                .orElseGet(TeamMembership::new);

        membership.setTeam(team);
        membership.setUser(user);
        membership.setRole(TeamMembershipRole.MEMBER);
        membership.setStatus(TeamMembershipStatus.ACTIVE);
        membership.setJoinedAt(Instant.now());

        teamMembershipRepository.save(membership);

        return buildTeamResponse(team);
    }

    @Transactional(readOnly = true)
    public TeamResponse getCurrentTeam(String email) {
        User user = getUserByEmail(email);

        TeamMembership membership = teamMembershipRepository.findByUserIdAndStatus(
                        user.getId(),
                        TeamMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new NotFoundException("У пользователя нет команды"));

        return buildTeamResponse(membership.getTeam());
    }

    private void validateCanCreateTeam(User user) {
        if (user.getRole() != Role.PARTICIPANT) {
            throw new BadRequestException("Создавать команды могут только участники");
        }

        if (teamMembershipRepository.existsByUserIdAndStatus(user.getId(), TeamMembershipStatus.ACTIVE)) {
            throw new ConflictException("Пользователь уже состоит в команде");
        }
    }

    private void validateCanJoinTeam(User user) {
        if (user.getRole() != Role.PARTICIPANT) {
            throw new BadRequestException("Вступать в команды могут только участники");
        }

        if (teamMembershipRepository.existsByUserIdAndStatus(user.getId(), TeamMembershipStatus.ACTIVE)) {
            throw new ConflictException("Пользователь уже состоит в команде");
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private TeamResponse buildTeamResponse(Team team) {
        List<TeamMemberResponse> members = teamMembershipRepository.findAllByTeamIdAndStatus(
                        team.getId(),
                        TeamMembershipStatus.ACTIVE
                ).stream()
                .map(this::buildTeamMemberResponse)
                .toList();

        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getCity(),
                team.getInviteCode(),
                team.getCaptain().getId(),
                team.getCaptain().getEmail(),
                team.getCreatedAt(),
                members
        );
    }

    private TeamMemberResponse buildTeamMemberResponse(TeamMembership membership) {
        return new TeamMemberResponse(
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getRole(),
                membership.getStatus(),
                membership.getJoinedAt()
        );
    }

    private String generateUniqueInviteCode() {
        SecureRandom random = new SecureRandom();
        String inviteCode;

        do {
            inviteCode = generateInviteCode(random);
        } while (teamRepository.existsByInviteCode(inviteCode));

        return inviteCode;
    }

    private String generateInviteCode(SecureRandom random) {
        StringBuilder builder = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            int index = random.nextInt(INVITE_CODE_ALPHABET.length());
            builder.append(INVITE_CODE_ALPHABET.charAt(index));
        }
        return builder.toString();
    }

    private String normalizeInviteCode(String inviteCode) {
        return inviteCode.trim().toUpperCase(Locale.ROOT);
    }
}
