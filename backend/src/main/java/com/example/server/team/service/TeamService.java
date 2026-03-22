package com.example.server.team.service;

import com.example.server.auth.entity.Role;
import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import com.example.server.common.exception.BadRequestException;
import com.example.server.common.exception.ConflictException;
import com.example.server.common.exception.NotFoundException;
import com.example.server.team.dto.CreateTeamRequest;
import com.example.server.team.dto.IncomingJoinRequestResponse;
import com.example.server.team.dto.JoinTeamByCodeRequest;
import com.example.server.team.dto.OutgoingJoinRequestResponse;
import com.example.server.team.dto.TeamJoinRequestDecisionResponse;
import com.example.server.team.dto.TeamMemberResponse;
import com.example.server.team.dto.TeamJoinRequestResponse;
import com.example.server.team.dto.TeamListItemResponse;
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

    @Transactional
    public TeamJoinRequestResponse createJoinRequest(String email, Long teamId) {
        User user = getUserByEmail(email);
        validateCanRequestToJoinTeam(user);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        if (team.getCaptain().getId().equals(user.getId())) {
            throw new ConflictException("Капитан уже состоит в этой команде");
        }

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                .orElseGet(TeamMembership::new);

        if (membership.getId() != null && membership.getStatus() == TeamMembershipStatus.PENDING) {
            throw new ConflictException("Заявка в эту команду уже отправлена");
        }

        membership.setTeam(team);
        membership.setUser(user);
        membership.setRole(TeamMembershipRole.MEMBER);
        membership.setStatus(TeamMembershipStatus.PENDING);
        membership.setJoinedAt(null);

        TeamMembership savedMembership = teamMembershipRepository.save(membership);

        return new TeamJoinRequestResponse(
                team.getId(),
                team.getName(),
                team.getCity(),
                savedMembership.getStatus(),
                savedMembership.getCreatedAt()
        );
    }

    @Transactional
    public void cancelJoinRequest(String email, Long teamId) {
        User user = getUserByEmail(email);

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new NotFoundException("Заявка не найдена"));

        if (membership.getStatus() != TeamMembershipStatus.PENDING) {
            throw new BadRequestException("Можно отменить только заявку в статусе PENDING");
        }

        membership.setStatus(TeamMembershipStatus.LEFT);
        membership.setJoinedAt(null);
        teamMembershipRepository.save(membership);
    }

    @Transactional
    public TeamJoinRequestDecisionResponse approveJoinRequest(String captainEmail, Long teamId, Long userId) {
        resolveCaptainMembership(captainEmail, teamId);
        TeamMembership membership = getPendingMembership(teamId, userId);

        if (teamMembershipRepository.existsByUserIdAndStatus(userId, TeamMembershipStatus.ACTIVE)) {
            throw new ConflictException("Пользователь уже состоит в команде");
        }

        membership.setStatus(TeamMembershipStatus.ACTIVE);
        membership.setJoinedAt(Instant.now());

        TeamMembership savedMembership = teamMembershipRepository.save(membership);
        return buildJoinRequestDecisionResponse(savedMembership);
    }

    @Transactional
    public TeamJoinRequestDecisionResponse rejectJoinRequest(String captainEmail, Long teamId, Long userId) {
        resolveCaptainMembership(captainEmail, teamId);
        TeamMembership membership = getPendingMembership(teamId, userId);

        membership.setStatus(TeamMembershipStatus.REJECTED);
        membership.setJoinedAt(null);

        TeamMembership savedMembership = teamMembershipRepository.save(membership);
        return buildJoinRequestDecisionResponse(savedMembership);
    }

    @Transactional
    public void removeMember(String captainEmail, Long teamId, Long userId) {
        TeamMembership captainMembership = resolveCaptainMembership(captainEmail, teamId);

        if (captainMembership.getUser().getId().equals(userId)) {
            throw new BadRequestException("Капитан не может исключить сам себя");
        }

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new NotFoundException("Участник команды не найден"));

        if (membership.getStatus() != TeamMembershipStatus.ACTIVE) {
            throw new BadRequestException("Можно исключить только активного участника команды");
        }

        if (membership.getRole() == TeamMembershipRole.CAPTAIN) {
            throw new BadRequestException("Нельзя исключить капитана команды");
        }

        membership.setStatus(TeamMembershipStatus.LEFT);
        membership.setJoinedAt(null);
        teamMembershipRepository.save(membership);
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

    @Transactional(readOnly = true)
    public List<TeamListItemResponse> getTeams(String city) {
        List<Team> teams = city == null || city.isBlank()
                ? teamRepository.findAllByOrderByCreatedAtDesc()
                : teamRepository.findAllByCityIgnoreCaseOrderByCreatedAtDesc(city.trim());

        return teams.stream()
                .map(this::buildTeamListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        return buildTeamResponse(team);
    }

    @Transactional(readOnly = true)
    public List<IncomingJoinRequestResponse> getIncomingJoinRequests(String captainEmail) {
        TeamMembership captainMembership = resolveCaptainMembership(captainEmail);

        return teamMembershipRepository.findAllByTeamIdAndStatus(
                        captainMembership.getTeam().getId(),
                        TeamMembershipStatus.PENDING
                ).stream()
                .map(this::buildIncomingJoinRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OutgoingJoinRequestResponse> getOutgoingJoinRequests(String userEmail) {
        User user = getUserByEmail(userEmail);

        return teamMembershipRepository.findAllByUserIdAndStatus(user.getId(), TeamMembershipStatus.PENDING).stream()
                .map(this::buildOutgoingJoinRequestResponse)
                .toList();
    }

    @Transactional
    public void leaveTeam(String email) {
        User user = getUserByEmail(email);
        TeamMembership membership = teamMembershipRepository.findByUserIdAndStatus(user.getId(), TeamMembershipStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("У пользователя нет команды"));

        if (membership.getRole() == TeamMembershipRole.CAPTAIN) {
            validateCaptainCanLeave(membership.getTeam());
            membership.setStatus(TeamMembershipStatus.LEFT);
            membership.setJoinedAt(null);
            teamMembershipRepository.save(membership);
            teamRepository.delete(membership.getTeam());
            return;
        }

        membership.setStatus(TeamMembershipStatus.LEFT);
        membership.setJoinedAt(null);
        teamMembershipRepository.save(membership);
    }

    private void validateCanCreateTeam(User user) {
        if (user.getRole() != Role.PARTICIPANT) {
            throw new BadRequestException("Создавать команды могут только участники");
        }

        validateNoBlockingMemberships(user);
    }

    private void validateCanJoinTeam(User user) {
        if (user.getRole() != Role.PARTICIPANT) {
            throw new BadRequestException("Вступать в команды могут только участники");
        }

        validateNoBlockingMemberships(user);
    }

    private void validateCanRequestToJoinTeam(User user) {
        if (user.getRole() != Role.PARTICIPANT) {
            throw new BadRequestException("Отправлять заявки в команды могут только участники");
        }

        validateNoBlockingMemberships(user);
    }

    private void validateNoBlockingMemberships(User user) {
        if (teamMembershipRepository.existsByUserIdAndStatus(user.getId(), TeamMembershipStatus.ACTIVE)) {
            throw new ConflictException("Пользователь уже состоит в команде");
        }

        if (!teamMembershipRepository.findAllByUserIdAndStatus(user.getId(), TeamMembershipStatus.PENDING).isEmpty()) {
            throw new ConflictException("У пользователя уже есть необработанная заявка в команду");
        }
    }

    private TeamMembership resolveCaptainMembership(String captainEmail, Long teamId) {
        User captain = getUserByEmail(captainEmail);

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(teamId, captain.getId())
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        if (membership.getStatus() != TeamMembershipStatus.ACTIVE
                || membership.getRole() != TeamMembershipRole.CAPTAIN) {
            throw new BadRequestException("Только капитан команды может обрабатывать заявки");
        }

        return membership;
    }

    private TeamMembership resolveCaptainMembership(String captainEmail) {
        User captain = getUserByEmail(captainEmail);

        TeamMembership membership = teamMembershipRepository.findByUserIdAndStatus(captain.getId(), TeamMembershipStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("У пользователя нет команды"));

        if (membership.getRole() != TeamMembershipRole.CAPTAIN) {
            throw new BadRequestException("Только капитан команды может просматривать входящие заявки");
        }

        return membership;
    }

    private void validateCaptainCanLeave(Team team) {
        long activeMembers = teamMembershipRepository.countByTeamIdAndStatus(team.getId(), TeamMembershipStatus.ACTIVE);
        long pendingMembers = teamMembershipRepository.countByTeamIdAndStatus(team.getId(), TeamMembershipStatus.PENDING);

        if (activeMembers > 1) {
            throw new BadRequestException("Капитан не может выйти из команды, пока в ней есть другие активные участники");
        }

        if (pendingMembers > 0) {
            throw new BadRequestException("Капитан не может выйти из команды, пока есть необработанные заявки");
        }
    }

    private TeamMembership getPendingMembership(Long teamId, Long userId) {
        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена"));

        if (membership.getStatus() != TeamMembershipStatus.PENDING) {
            throw new BadRequestException("Можно обработать только заявку в статусе PENDING");
        }

        return membership;
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

    private TeamJoinRequestDecisionResponse buildJoinRequestDecisionResponse(TeamMembership membership) {
        return new TeamJoinRequestDecisionResponse(
                membership.getTeam().getId(),
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getStatus(),
                membership.getJoinedAt(),
                membership.getUpdatedAt()
        );
    }

    private IncomingJoinRequestResponse buildIncomingJoinRequestResponse(TeamMembership membership) {
        return new IncomingJoinRequestResponse(
                membership.getTeam().getId(),
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getStatus(),
                membership.getCreatedAt()
        );
    }

    private OutgoingJoinRequestResponse buildOutgoingJoinRequestResponse(TeamMembership membership) {
        return new OutgoingJoinRequestResponse(
                membership.getTeam().getId(),
                membership.getTeam().getName(),
                membership.getTeam().getCity(),
                membership.getStatus(),
                membership.getCreatedAt(),
                membership.getUpdatedAt()
        );
    }

    private TeamListItemResponse buildTeamListItemResponse(Team team) {
        int activeMembersCount = teamMembershipRepository.findAllByTeamIdAndStatus(
                        team.getId(),
                        TeamMembershipStatus.ACTIVE
                ).size();

        return new TeamListItemResponse(
                team.getId(),
                team.getName(),
                team.getCity(),
                team.getCaptain().getId(),
                team.getCaptain().getEmail(),
                activeMembersCount,
                team.getCreatedAt()
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
