package com.example.server.unit.team;

import com.example.server.auth.entity.Role;
import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import com.example.server.common.exception.BadRequestException;
import com.example.server.common.exception.ConflictException;
import com.example.server.common.exception.NotFoundException;
import com.example.server.team.dto.*;
import com.example.server.team.entity.*;
import com.example.server.team.repository.TeamMembershipRepository;
import com.example.server.team.repository.TeamRepository;
import com.example.server.team.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMembershipRepository membershipRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private TeamService teamService;

    // ─── helpers ──────────────────────────────────────────────────────────────

    private User makeUser(Long id, String email, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    private Team makeTeam(Long id, User captain) {
        Team t = new Team();
        t.setId(id);
        t.setName("Team");
        t.setCity("Moscow");
        t.setInviteCode("ABCD1234");
        t.setCaptain(captain);
        return t;
    }

    private TeamMembership makeMembership(Long id, Team team, User user,
                                          TeamMembershipRole role, TeamMembershipStatus status) {
        TeamMembership m = new TeamMembership();
        m.setId(id);
        m.setTeam(team);
        m.setUser(user);
        m.setRole(role);
        m.setStatus(status);
        return m;
    }

    // ─── createTeam ───────────────────────────────────────────────────────────

    // №1 — Участник успешно создаёт команду и становится капитаном
    @Test
    void createTeam_success_returnsTeamResponseAndSavesMembership() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        Team saved = makeTeam(10L, user);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.existsByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndStatus(1L, TeamMembershipStatus.PENDING)).thenReturn(Collections.emptyList());
        when(teamRepository.existsByInviteCode(any())).thenReturn(false);
        when(teamRepository.save(any())).thenReturn(saved);
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findAllByTeamIdAndStatus(eq(10L), eq(TeamMembershipStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        TeamResponse resp = teamService.createTeam("user@test.com", new CreateTeamRequest("Team", "Moscow"));

        assertNotNull(resp);
        assertEquals(10L, resp.id());
        verify(membershipRepository).save(argThat(m ->
                m.getRole() == TeamMembershipRole.CAPTAIN &&
                        m.getStatus() == TeamMembershipStatus.ACTIVE));
    }

    // №2 — Организатор не может создать команду
    @Test
    void createTeam_organizerRole_throwsBadRequest() {
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));

        assertThrows(BadRequestException.class,
                () -> teamService.createTeam("org@test.com", new CreateTeamRequest("T", "C")));
    }

    // №3 — Нельзя создать команду, уже состоя в другой
    @Test
    void createTeam_alreadyActiveMember_throwsConflict() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.existsByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> teamService.createTeam("user@test.com", new CreateTeamRequest("T", "C")));
    }

    // №4 — Нельзя создать команду при наличии необработанной заявки
    @Test
    void createTeam_hasPendingRequest_throwsConflict() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        TeamMembership pending = makeMembership(1L, null, user, TeamMembershipRole.MEMBER, TeamMembershipStatus.PENDING);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.existsByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndStatus(1L, TeamMembershipStatus.PENDING))
                .thenReturn(List.of(pending));

        assertThrows(ConflictException.class,
                () -> teamService.createTeam("user@test.com", new CreateTeamRequest("T", "C")));
    }

    // ─── joinTeamByCode ───────────────────────────────────────────────────────

    // №1 — Участник успешно вступает в команду по коду
    @Test
    void joinTeamByCode_success_savesActiveMembership() {
        User user = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.existsByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndStatus(2L, TeamMembershipStatus.PENDING)).thenReturn(Collections.emptyList());
        when(teamRepository.findByInviteCode("ABCD1234")).thenReturn(Optional.of(team));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.empty());
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findAllByTeamIdAndStatus(eq(10L), eq(TeamMembershipStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        TeamResponse resp = teamService.joinTeamByCode("user@test.com",
                new JoinTeamByCodeRequest("ABCD1234"));

        assertNotNull(resp);
        verify(membershipRepository).save(argThat(m ->
                m.getStatus() == TeamMembershipStatus.ACTIVE));
    }

    // №2 — Код нормализуется: trim + toUpperCase до поиска
    @Test
    void joinTeamByCode_normalizesInviteCode() {
        User user = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.existsByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndStatus(2L, TeamMembershipStatus.PENDING)).thenReturn(Collections.emptyList());
        when(teamRepository.findByInviteCode("ABCD1234")).thenReturn(Optional.of(team));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.empty());
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findAllByTeamIdAndStatus(eq(10L), eq(TeamMembershipStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        teamService.joinTeamByCode("user@test.com", new JoinTeamByCodeRequest("  abcd1234  "));

        verify(teamRepository).findByInviteCode("ABCD1234");
    }

    // №3 — Неверный код приглашения выбрасывает исключение
    @Test
    void joinTeamByCode_invalidCode_throwsNotFound() {
        User user = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.existsByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndStatus(2L, TeamMembershipStatus.PENDING)).thenReturn(Collections.emptyList());
        when(teamRepository.findByInviteCode("BADCODE1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> teamService.joinTeamByCode("user@test.com", new JoinTeamByCodeRequest("BADCODE1")));
    }

    // №4 — Организатор не может вступить в команду
    @Test
    void joinTeamByCode_organizerRole_throwsBadRequest() {
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));

        assertThrows(BadRequestException.class,
                () -> teamService.joinTeamByCode("org@test.com", new JoinTeamByCodeRequest("ABCD1234")));
    }

    // №5 — Нельзя вступить в команду, уже состоя в другой
    @Test
    void joinTeamByCode_alreadyActiveMember_throwsConflict() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.existsByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> teamService.joinTeamByCode("user@test.com", new JoinTeamByCodeRequest("ABCD1234")));
    }

    // №6 — Капитан не может вступить в свою же команду по коду
    @Test
    void joinTeamByCode_captainJoinsOwnTeam_throwsConflict() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.existsByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndStatus(1L, TeamMembershipStatus.PENDING)).thenReturn(Collections.emptyList());
        when(teamRepository.findByInviteCode("ABCD1234")).thenReturn(Optional.of(team));

        assertThrows(ConflictException.class,
                () -> teamService.joinTeamByCode("cap@test.com", new JoinTeamByCodeRequest("ABCD1234")));
    }

    // ─── createJoinRequest ────────────────────────────────────────────────────

    // №1 — Участник успешно отправляет заявку в команду
    @Test
    void createJoinRequest_success_returnsPendingResponse() {
        User user = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.existsByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndStatus(2L, TeamMembershipStatus.PENDING)).thenReturn(Collections.emptyList());
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.empty());
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TeamJoinRequestResponse resp = teamService.createJoinRequest("user@test.com", 10L);

        assertEquals(TeamMembershipStatus.PENDING, resp.status());
    }

    // №2 — Организатор не может подавать заявки в команды
    @Test
    void createJoinRequest_organizerRole_throwsBadRequest() {
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));

        assertThrows(BadRequestException.class,
                () -> teamService.createJoinRequest("org@test.com", 10L));
    }

    // №3 — Заявка в несуществующую команду выбрасывает исключение
    @Test
    void createJoinRequest_teamNotFound_throwsNotFoundException() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.existsByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndStatus(1L, TeamMembershipStatus.PENDING)).thenReturn(Collections.emptyList());
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> teamService.createJoinRequest("user@test.com", 99L));
    }

    // №4 — Нельзя подать заявку, уже состоя в команде
    @Test
    void createJoinRequest_alreadyActiveMember_throwsConflict() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.existsByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> teamService.createJoinRequest("user@test.com", 10L));
    }

    // №5 — Повторная заявка в ту же команду выбрасывает исключение
    @Test
    void createJoinRequest_duplicatePendingRequest_throwsConflict() {
        User user = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership existing = makeMembership(5L, team, user, TeamMembershipRole.MEMBER, TeamMembershipStatus.PENDING);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.existsByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndStatus(2L, TeamMembershipStatus.PENDING)).thenReturn(Collections.emptyList());
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class,
                () -> teamService.createJoinRequest("user@test.com", 10L));
    }

    // №6 — Капитан не может подать заявку в свою команду
    @Test
    void createJoinRequest_captainJoinsOwnTeam_throwsConflict() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.existsByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.findAllByUserIdAndStatus(1L, TeamMembershipStatus.PENDING)).thenReturn(Collections.emptyList());
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        assertThrows(ConflictException.class,
                () -> teamService.createJoinRequest("cap@test.com", 10L));
    }

    // ─── cancelJoinRequest ────────────────────────────────────────────────────

    // №1 — Пользователь успешно отменяет свою заявку
    @Test
    void cancelJoinRequest_success_setsStatusLeft() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, user);
        TeamMembership membership = makeMembership(5L, team, user, TeamMembershipRole.MEMBER, TeamMembershipStatus.PENDING);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        teamService.cancelJoinRequest("user@test.com", 10L);

        assertEquals(TeamMembershipStatus.LEFT, membership.getStatus());
        assertNull(membership.getJoinedAt());
    }

    // №2 — Отмена несуществующей заявки выбрасывает исключение
    @Test
    void cancelJoinRequest_membershipNotFound_throwsNotFoundException() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> teamService.cancelJoinRequest("user@test.com", 10L));
    }

    // №3 — Нельзя отменить заявку, которая уже не в статусе PENDING
    @Test
    void cancelJoinRequest_notPendingStatus_throwsBadRequest() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, user);
        TeamMembership membership = makeMembership(5L, team, user, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));

        assertThrows(BadRequestException.class,
                () -> teamService.cancelJoinRequest("user@test.com", 10L));
    }

    // ─── approveJoinRequest ───────────────────────────────────────────────────

    // №1 — Капитан успешно принимает заявку
    @Test
    void approveJoinRequest_success_setsActiveStatus() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        User applicant = makeUser(2L, "app@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        TeamMembership pendingMembership = makeMembership(2L, team, applicant, TeamMembershipRole.MEMBER, TeamMembershipStatus.PENDING);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(pendingMembership));
        when(membershipRepository.existsByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE)).thenReturn(false);
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TeamJoinRequestDecisionResponse resp = teamService.approveJoinRequest("cap@test.com", 10L, 2L);

        assertEquals(TeamMembershipStatus.ACTIVE, resp.status());
        assertNotNull(resp.joinedAt());
    }

    // №2 — Принять заявку может только капитан
    @Test
    void approveJoinRequest_notCaptain_throwsBadRequest() {
        User member = makeUser(1L, "mem@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, member);
        TeamMembership membership = makeMembership(1L, team, member, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("mem@test.com")).thenReturn(Optional.of(member));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));

        assertThrows(BadRequestException.class,
                () -> teamService.approveJoinRequest("mem@test.com", 10L, 2L));
    }

    // №3 — Нельзя принять заявку не в статусе PENDING
    @Test
    void approveJoinRequest_notPending_throwsBadRequest() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        User applicant = makeUser(2L, "app@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        TeamMembership activeMembership = makeMembership(2L, team, applicant, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(activeMembership));

        assertThrows(BadRequestException.class,
                () -> teamService.approveJoinRequest("cap@test.com", 10L, 2L));
    }

    // №4 — Нельзя принять заявку пользователя, уже состоящего в команде
    @Test
    void approveJoinRequest_applicantAlreadyActive_throwsConflict() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        User applicant = makeUser(2L, "app@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        TeamMembership pendingMembership = makeMembership(2L, team, applicant, TeamMembershipRole.MEMBER, TeamMembershipStatus.PENDING);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(pendingMembership));
        when(membershipRepository.existsByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> teamService.approveJoinRequest("cap@test.com", 10L, 2L));
    }

    // ─── rejectJoinRequest ────────────────────────────────────────────────────

    // №1 — Капитан успешно отклоняет заявку
    @Test
    void rejectJoinRequest_success_setsRejectedStatus() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        User applicant = makeUser(2L, "app@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        TeamMembership pendingMembership = makeMembership(2L, team, applicant, TeamMembershipRole.MEMBER, TeamMembershipStatus.PENDING);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(pendingMembership));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TeamJoinRequestDecisionResponse resp = teamService.rejectJoinRequest("cap@test.com", 10L, 2L);

        assertEquals(TeamMembershipStatus.REJECTED, resp.status());
        assertNull(resp.joinedAt());
    }

    // №2 — Отклонить заявку может только капитан
    @Test
    void rejectJoinRequest_notCaptain_throwsBadRequest() {
        User member = makeUser(1L, "mem@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, member);
        TeamMembership membership = makeMembership(1L, team, member, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("mem@test.com")).thenReturn(Optional.of(member));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));

        assertThrows(BadRequestException.class,
                () -> teamService.rejectJoinRequest("mem@test.com", 10L, 2L));
    }

    // №3 — Нельзя отклонить заявку не в статусе PENDING
    @Test
    void rejectJoinRequest_notPending_throwsBadRequest() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        User applicant = makeUser(2L, "app@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        TeamMembership activeMembership = makeMembership(2L, team, applicant, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(activeMembership));

        assertThrows(BadRequestException.class,
                () -> teamService.rejectJoinRequest("cap@test.com", 10L, 2L));
    }

    // ─── removeMember ─────────────────────────────────────────────────────────

    // №1 — Капитан успешно исключает участника
    @Test
    void removeMember_success_setsStatusLeft() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        User member = makeUser(2L, "mem@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        TeamMembership memberMembership = makeMembership(2L, team, member, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(memberMembership));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        teamService.removeMember("cap@test.com", 10L, 2L);

        assertEquals(TeamMembershipStatus.LEFT, memberMembership.getStatus());
        assertNull(memberMembership.getJoinedAt());
    }

    // №2 — Капитан не может исключить сам себя
    @Test
    void removeMember_captainRemovesSelf_throwsBadRequest() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));

        assertThrows(BadRequestException.class,
                () -> teamService.removeMember("cap@test.com", 10L, 1L));
    }

    // №3 — Можно исключить только активного участника
    @Test
    void removeMember_memberNotActive_throwsBadRequest() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        User member = makeUser(2L, "mem@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        TeamMembership leftMembership = makeMembership(2L, team, member, TeamMembershipRole.MEMBER, TeamMembershipStatus.LEFT);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(leftMembership));

        assertThrows(BadRequestException.class,
                () -> teamService.removeMember("cap@test.com", 10L, 2L));
    }

    // №4 — Нельзя исключить капитана команды
    @Test
    void removeMember_targetIsCaptain_throwsBadRequest() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        User coCaptain = makeUser(2L, "cap2@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        TeamMembership coCaptainMembership = makeMembership(2L, team, coCaptain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(coCaptainMembership));

        assertThrows(BadRequestException.class,
                () -> teamService.removeMember("cap@test.com", 10L, 2L));
    }

    // ─── transferCaptainRole ──────────────────────────────────────────────────

    // №1 — Капитан передаёт роль активному участнику
    @Test
    void transferCaptainRole_success_updatesRolesAndTeamCaptain() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        User newCaptain = makeUser(2L, "new@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        TeamMembership newCaptainMembership = makeMembership(2L, team, newCaptain, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(newCaptainMembership));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(teamRepository.save(any())).thenReturn(team);
        when(membershipRepository.findAllByTeamIdAndStatus(eq(10L), eq(TeamMembershipStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        teamService.transferCaptainRole("cap@test.com", 10L, 2L);

        assertEquals(TeamMembershipRole.MEMBER, captainMembership.getRole());
        assertEquals(TeamMembershipRole.CAPTAIN, newCaptainMembership.getRole());
        assertEquals(newCaptain, team.getCaptain());
    }

    // №2 — Нельзя передать роль капитана самому себе
    @Test
    void transferCaptainRole_sameUser_throwsBadRequest() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));

        assertThrows(BadRequestException.class,
                () -> teamService.transferCaptainRole("cap@test.com", 10L, 1L));
    }

    // №3 — Нельзя передать роль участнику не из этой команды
    @Test
    void transferCaptainRole_newCaptainNotFound_throwsNotFoundException() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findByTeamIdAndUserId(10L, 99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> teamService.transferCaptainRole("cap@test.com", 10L, 99L));
    }

    // №4 — Капитаном можно назначить только активного участника
    @Test
    void transferCaptainRole_newCaptainNotActive_throwsBadRequest() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        User inactive = makeUser(2L, "inactive@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        TeamMembership leftMembership = makeMembership(2L, team, inactive, TeamMembershipRole.MEMBER, TeamMembershipStatus.LEFT);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(leftMembership));

        assertThrows(BadRequestException.class,
                () -> teamService.transferCaptainRole("cap@test.com", 10L, 2L));
    }

    // ─── leaveTeam ────────────────────────────────────────────────────────────

    // №1 — Участник успешно покидает команду
    @Test
    void leaveTeam_success_setsStatusLeft() {
        User member = makeUser(1L, "mem@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, new User());
        TeamMembership membership = makeMembership(1L, team, member, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("mem@test.com")).thenReturn(Optional.of(member));
        when(membershipRepository.findByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        teamService.leaveTeam("mem@test.com");

        assertEquals(TeamMembershipStatus.LEFT, membership.getStatus());
        assertNull(membership.getJoinedAt());
    }

    // №2 — Капитан не может выйти без передачи роли
    @Test
    void leaveTeam_captain_throwsBadRequest() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership membership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));

        assertThrows(BadRequestException.class, () -> teamService.leaveTeam("cap@test.com"));
    }

    // №3 — Пользователь без команды получает исключение
    @Test
    void leaveTeam_noActiveTeam_throwsNotFoundException() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> teamService.leaveTeam("user@test.com"));
    }

    // ─── getCurrentTeam ───────────────────────────────────────────────────────

    // №1 — Возвращает данные команды текущего пользователя
    @Test
    void getCurrentTeam_activeMembership_returnsTeamResponse() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        User captain = makeUser(2L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership membership = makeMembership(1L, team, user, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(membershipRepository.findAllByTeamIdAndStatus(eq(10L), eq(TeamMembershipStatus.ACTIVE)))
                .thenReturn(List.of(membership));

        TeamResponse resp = teamService.getCurrentTeam("user@test.com");

        assertEquals(10L, resp.id());
        assertEquals("Team", resp.name());
        assertEquals("Moscow", resp.city());
    }

    // №2 — Пользователь без команды получает исключение
    @Test
    void getCurrentTeam_noActiveTeam_throwsNotFoundException() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> teamService.getCurrentTeam("user@test.com"));
    }

    // ─── updateTeam ───────────────────────────────────────────────────────────

    // №1 — Капитан обновляет название и город без смены кода
    @Test
    void updateTeam_success_updatesNameAndCity() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findAllByTeamIdAndStatus(eq(10L), eq(TeamMembershipStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        TeamResponse resp = teamService.updateTeam("cap@test.com", 10L,
                new UpdateTeamRequest("NewName", "Moscow", false));

        assertEquals("NewName", resp.name());
        assertEquals("Moscow", resp.city());
        assertEquals("ABCD1234", resp.inviteCode()); // код не изменился
    }

    // №2 — При regenerateInviteCode=true генерируется новый уникальный код
    @Test
    void updateTeam_regenerateInviteCode_codeChanges() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(teamRepository.existsByInviteCode(any())).thenReturn(false);
        when(teamRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.findAllByTeamIdAndStatus(eq(10L), eq(TeamMembershipStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        TeamResponse resp = teamService.updateTeam("cap@test.com", 10L,
                new UpdateTeamRequest("Team", "Moscow", true));

        assertNotEquals("ABCD1234", resp.inviteCode());
    }

    // №3 — Обновить команду может только капитан
    @Test
    void updateTeam_notCaptain_throwsBadRequest() {
        User member = makeUser(1L, "mem@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, member);
        TeamMembership membership = makeMembership(1L, team, member, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("mem@test.com")).thenReturn(Optional.of(member));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));

        assertThrows(BadRequestException.class,
                () -> teamService.updateTeam("mem@test.com", 10L,
                        new UpdateTeamRequest("T", "C", false)));
    }

    // №4 — Нельзя обновить чужую команду
    @Test
    void updateTeam_notInTeam_throwsNotFoundException() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> teamService.updateTeam("user@test.com", 10L,
                        new UpdateTeamRequest("T", "C", false)));
    }

    // ─── disbandTeam ──────────────────────────────────────────────────────────

    // №1 — Капитан успешно расформировывает команду
    @Test
    void disbandTeam_success_deletesAllMembershipsAndTeam() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findAllByTeamId(10L)).thenReturn(List.of(captainMembership));

        teamService.disbandTeam("cap@test.com", 10L);

        verify(membershipRepository).deleteAll(List.of(captainMembership));
        verify(teamRepository).delete(team);
    }

    // №2 — Расформировать команду может только капитан
    @Test
    void disbandTeam_notCaptain_throwsBadRequest() {
        User member = makeUser(1L, "mem@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, member);
        TeamMembership membership = makeMembership(1L, team, member, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("mem@test.com")).thenReturn(Optional.of(member));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));

        assertThrows(BadRequestException.class,
                () -> teamService.disbandTeam("mem@test.com", 10L));
    }

    // №3 — Нельзя расформировать чужую команду
    @Test
    void disbandTeam_notInTeam_throwsNotFoundException() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> teamService.disbandTeam("user@test.com", 10L));
    }

    // ─── getTeams ─────────────────────────────────────────────────────────────

    // №1 — Без фильтра возвращаются все команды
    @Test
    void getTeams_nullCity_returnsAllTeams() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        when(teamRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(team));
        when(membershipRepository.findAllByTeamIdAndStatus(eq(10L), eq(TeamMembershipStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        List<TeamListItemResponse> result = teamService.getTeams(null);

        assertEquals(1, result.size());
        verify(teamRepository).findAllByOrderByCreatedAtDesc();
        verify(teamRepository, never()).findAllByCityIgnoreCaseOrderByCreatedAtDesc(any());
    }

    // №2 — Пустая строка трактуется как отсутствие фильтра
    @Test
    void getTeams_blankCity_returnsAllTeams() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        when(teamRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(team));
        when(membershipRepository.findAllByTeamIdAndStatus(eq(10L), eq(TeamMembershipStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        List<TeamListItemResponse> result = teamService.getTeams("   ");

        assertEquals(1, result.size());
        verify(teamRepository).findAllByOrderByCreatedAtDesc();
    }

    // №3 — Фильтрация по городу возвращает только команды из этого города
    @Test
    void getTeams_withCity_returnsFilteredTeams() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        when(teamRepository.findAllByCityIgnoreCaseOrderByCreatedAtDesc("Moscow")).thenReturn(List.of(team));
        when(membershipRepository.findAllByTeamIdAndStatus(eq(10L), eq(TeamMembershipStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        List<TeamListItemResponse> result = teamService.getTeams("Moscow");

        assertEquals(1, result.size());
        verify(teamRepository).findAllByCityIgnoreCaseOrderByCreatedAtDesc("Moscow");
        verify(teamRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    // ─── getTeamById ──────────────────────────────────────────────────────────

    // №1 — Возвращает данные существующей команды по id
    @Test
    void getTeamById_exists_returnsTeamResponse() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(membershipRepository.findAllByTeamIdAndStatus(eq(10L), eq(TeamMembershipStatus.ACTIVE)))
                .thenReturn(Collections.emptyList());

        TeamResponse resp = teamService.getTeamById(10L);

        assertEquals(10L, resp.id());
        assertEquals("Team", resp.name());
    }

    // №2 — Запрос несуществующей команды выбрасывает исключение
    @Test
    void getTeamById_notFound_throwsNotFoundException() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> teamService.getTeamById(99L));
    }

    // ─── getIncomingJoinRequests ──────────────────────────────────────────────

    // №1 — Капитан получает список входящих заявок
    @Test
    void getIncomingJoinRequests_hasPendingRequests_returnsList() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        User applicant = makeUser(2L, "app@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        TeamMembership pendingMembership = makeMembership(2L, team, applicant, TeamMembershipRole.MEMBER, TeamMembershipStatus.PENDING);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findAllByTeamIdAndStatus(10L, TeamMembershipStatus.PENDING))
                .thenReturn(List.of(pendingMembership));

        List<IncomingJoinRequestResponse> result = teamService.getIncomingJoinRequests("cap@test.com");

        assertEquals(1, result.size());
        assertEquals(TeamMembershipStatus.PENDING, result.get(0).status());
    }

    // №2 — Если заявок нет — возвращается пустой список
    @Test
    void getIncomingJoinRequests_noPendingRequests_returnsEmptyList() {
        User captain = makeUser(1L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMembership = makeMembership(1L, team, captain, TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(membershipRepository.findByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMembership));
        when(membershipRepository.findAllByTeamIdAndStatus(10L, TeamMembershipStatus.PENDING))
                .thenReturn(Collections.emptyList());

        List<IncomingJoinRequestResponse> result = teamService.getIncomingJoinRequests("cap@test.com");

        assertTrue(result.isEmpty());
    }

    // №3 — Просматривать входящие заявки может только капитан
    @Test
    void getIncomingJoinRequests_notCaptain_throwsBadRequest() {
        User member = makeUser(1L, "mem@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, member);
        TeamMembership membership = makeMembership(1L, team, member, TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("mem@test.com")).thenReturn(Optional.of(member));
        when(membershipRepository.findByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));

        assertThrows(BadRequestException.class,
                () -> teamService.getIncomingJoinRequests("mem@test.com"));
    }

    // №4 — Пользователь без команды не может просматривать заявки
    @Test
    void getIncomingJoinRequests_noActiveTeam_throwsNotFoundException() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findByUserIdAndStatus(1L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> teamService.getIncomingJoinRequests("user@test.com"));
    }

    // ─── getOutgoingJoinRequests ──────────────────────────────────────────────

    // №1 — Пользователь получает список своих исходящих заявок
    @Test
    void getOutgoingJoinRequests_hasPendingRequests_returnsList() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        User captain = makeUser(2L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        TeamMembership pending = makeMembership(1L, team, user, TeamMembershipRole.MEMBER, TeamMembershipStatus.PENDING);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findAllByUserIdAndStatus(1L, TeamMembershipStatus.PENDING))
                .thenReturn(List.of(pending));

        List<OutgoingJoinRequestResponse> result = teamService.getOutgoingJoinRequests("user@test.com");

        assertEquals(1, result.size());
        assertEquals(TeamMembershipStatus.PENDING, result.get(0).status());
    }

    // №2 — Если заявок нет — возвращается пустой список
    @Test
    void getOutgoingJoinRequests_noPendingRequests_returnsEmptyList() {
        User user = makeUser(1L, "user@test.com", Role.PARTICIPANT);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(membershipRepository.findAllByUserIdAndStatus(1L, TeamMembershipStatus.PENDING))
                .thenReturn(Collections.emptyList());

        List<OutgoingJoinRequestResponse> result = teamService.getOutgoingJoinRequests("user@test.com");

        assertTrue(result.isEmpty());
    }

    // №3 — Запрос для несуществующего пользователя выбрасывает исключение
    @Test
    void getOutgoingJoinRequests_userNotFound_throwsNotFoundException() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> teamService.getOutgoingJoinRequests("missing@test.com"));
    }
}