package com.example.server.unit.game;

import com.example.server.auth.entity.Role;
import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import com.example.server.common.exception.BadRequestException;
import com.example.server.common.exception.NotFoundException;
import com.example.server.game.dto.*;
import com.example.server.game.entity.*;
import com.example.server.game.repository.*;
import com.example.server.game.service.GameService;
import com.example.server.team.entity.*;
import com.example.server.team.repository.TeamMembershipRepository;
import com.example.server.team.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceGameplayTest {

    @Mock private GameRepository gameRepository;
    @Mock private GameRegistrationRepository gameRegistrationRepository;
    @Mock private GameTeamSessionRepository gameTeamSessionRepository;
    @Mock private GameChatMessageRepository gameChatMessageRepository;
    @Mock private GameTaskRepository gameTaskRepository;
    @Mock private GameTaskHintRepository gameTaskHintRepository;
    @Mock private TeamGameRouteRepository teamGameRouteRepository;
    @Mock private TeamGameRouteItemRepository teamGameRouteItemRepository;
    @Mock private TeamMembershipRepository teamMembershipRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private GameService gameService;

    private static final Long GAME_ID = 1L;

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
        t.setName("Test Team");
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

    private Game makeInProgressGame(Long id, User organizer) {
        Game g = new Game();
        g.setId(id);
        g.setTitle("Active Quest");
        g.setDescription("Desc");
        g.setCity("Moscow");
        g.setStatus(GameStatus.IN_PROGRESS);
        g.setMinTeamSize(2);
        g.setMaxTeamSize(4);
        g.setTaskFailurePenaltyMinutes(5);
        g.setRouteSlotsCount(2);
        g.setRegistrationStartsAt(Instant.now().minus(48, ChronoUnit.HOURS));
        g.setRegistrationEndsAt(Instant.now().minus(24, ChronoUnit.HOURS));
        g.setStartsAt(Instant.now().minus(1, ChronoUnit.HOURS));
        g.setOrganizer(organizer);
        return g;
    }

    private GameTask makeTask(Long id, Game game, String key, Integer timeLimitMinutes) {
        GameTask t = new GameTask();
        t.setId(id);
        t.setGame(game);
        t.setTitle("Task " + id);
        t.setRiddleText("Riddle");
        t.setAnswerKey(key);
        t.setOrderIndex(1);
        t.setTimeLimitMinutes(timeLimitMinutes);
        t.setFailurePenaltyMinutes(5);
        return t;
    }

    private TeamGameRoute makeRoute(Long id, Game game) {
        TeamGameRoute r = new TeamGameRoute();
        r.setId(id);
        r.setGame(game);
        r.setSlotNumber(1);
        r.setName("Route A");
        return r;
    }

    private TeamGameRouteItem makeRouteItem(Long id, TeamGameRoute route, GameTask task, Integer orderIndex) {
        TeamGameRouteItem item = new TeamGameRouteItem();
        item.setId(id);
        item.setRoute(route);
        item.setTask(task);
        item.setOrderIndex(orderIndex);
        return item;
    }

    private GameTeamSession makeSession(Long id, Game game, Team team,
                                        TeamGameRoute route, TeamGameRouteItem routeItem,
                                        GameTask task) {
        GameTeamSession s = new GameTeamSession();
        s.setId(id);
        s.setGame(game);
        s.setTeam(team);
        s.setRoute(route);
        s.setCurrentRouteItem(routeItem);
        s.setCurrentTask(task);
        s.setCurrentOrderIndex(routeItem.getOrderIndex());
        s.setStatus(GameTeamSessionStatus.IN_PROGRESS);
        s.setStartedAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        s.setCurrentTaskStartedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
        s.setTotalPenaltyMinutes(0);
        return s;
    }

    private GameRegistration makeApprovedRegistration(Long id, Game game, Team team) {
        GameRegistration r = new GameRegistration();
        r.setId(id);
        r.setGame(game);
        r.setTeam(team);
        r.setStatus(GameRegistrationStatus.APPROVED);
        return r;
    }

    /** Синхронизация игры IN_PROGRESS: existsByGameIdAndStatus(IN_PROGRESS) = true → статус не меняется */
    private void stubInProgressSessions(Long gameId) {
        when(gameTeamSessionRepository.existsByGameIdAndStatus(gameId, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(true);
    }

    // ─── getCurrentTask ───────────────────────────────────────────────────────

    @Test // №1 — Успешное получение текущего задания команды
    void getCurrentTask_success_returnsTaskResponse() {
        User participant = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, participant);
        TeamMembership membership = makeMembership(1L, team, participant,
                TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        // timeLimitMinutes = 60 → deadline is в будущем
        GameTask task = makeTask(5L, game, "ANSWER", 60);
        TeamGameRoute route = makeRoute(20L, game);
        TeamGameRouteItem routeItem = makeRouteItem(30L, route, task, 1);
        GameTeamSession session = makeSession(100L, game, team, route, routeItem, task);

        // synchronizeTeamGamesLifecycle → gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc(10L))
                .thenReturn(Collections.emptyList());
        when(gameTeamSessionRepository.findByTeamIdAndStatus(10L, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(session));
        // synchronizeSessionWithTimeout: deadline = currentTaskStartedAt + 60min → в будущем → выходит сразу
        when(teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(20L))
                .thenReturn(List.of(routeItem));
        when(gameTaskHintRepository.findAllByTaskIdOrderByOrderIndexAsc(5L))
                .thenReturn(Collections.emptyList());

        CurrentGameTaskResponse resp = gameService.getCurrentTask("user@test.com");

        assertEquals(100L, resp.sessionId());
        assertEquals(5L, resp.taskId());
    }

    @Test // №2 — Нет активной игровой сессии → NotFoundException
    void getCurrentTask_noActiveSession_throwsNotFoundException() {
        User participant = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, participant);
        TeamMembership membership = makeMembership(1L, team, participant,
                TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc(10L))
                .thenReturn(Collections.emptyList());
        when(gameTeamSessionRepository.findByTeamIdAndStatus(10L, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> gameService.getCurrentTask("user@test.com"));
    }

    @Test // №3 — Нет активной команды → NotFoundException
    void getCurrentTask_noActiveTeam_throwsNotFoundException() {
        User participant = makeUser(2L, "user@test.com", Role.PARTICIPANT);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> gameService.getCurrentTask("user@test.com"));
    }

    // ─── submitTaskKey ────────────────────────────────────────────────────────

    @Test // №4 — Правильный ключ, последнее задание → сессия завершена
    void submitTaskKey_correctKeyLastTask_finishesSession() {
        User captain = makeUser(2L, "cap@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMs = makeMembership(1L, team, captain,
                TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameTask task = makeTask(5L, game, "ANSWER", 60);
        TeamGameRoute route = makeRoute(20L, game);
        TeamGameRouteItem routeItem = makeRouteItem(30L, route, task, 1);
        GameTeamSession session = makeSession(100L, game, team, route, routeItem, task);
        GameTeamSession savedSession = makeSession(100L, game, team, route, routeItem, task);
        savedSession.setStatus(GameTeamSessionStatus.FINISHED);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(captainMs));
        when(gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc(10L))
                .thenReturn(Collections.emptyList());
        when(gameTeamSessionRepository.findByTeamIdAndStatus(10L, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(session));
        // synchronizeSessionWithTimeout: deadline в будущем → returns immediately
        when(teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(20L))
                .thenReturn(List.of(routeItem)); // только 1 элемент → следующего нет
        when(gameTeamSessionRepository.save(any(GameTeamSession.class))).thenReturn(savedSession);
        when(gameTeamSessionRepository.existsByGameIdAndStatus(GAME_ID, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(false); // больше нет активных сессий → игра завершается
        when(gameRepository.save(any(Game.class))).thenReturn(game);

        SubmitTaskKeyResponse resp = gameService.submitTaskKey("cap@test.com",
                new SubmitTaskKeyRequest("ANSWER"));

        assertTrue(resp.gameSessionFinished());
        verify(gameTeamSessionRepository).save(any(GameTeamSession.class));
    }

    @Test // №5 — Правильный ключ, есть следующее задание → переход к следующему
    void submitTaskKey_correctKeyHasNextTask_advancesToNextTask() {
        User captain = makeUser(2L, "cap@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMs = makeMembership(1L, team, captain,
                TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameTask task1 = makeTask(5L, game, "KEY1", 60);
        task1.setOrderIndex(1);
        GameTask task2 = makeTask(6L, game, "KEY2", 60);
        task2.setOrderIndex(2);
        TeamGameRoute route = makeRoute(20L, game);
        TeamGameRouteItem item1 = makeRouteItem(30L, route, task1, 1);
        TeamGameRouteItem item2 = makeRouteItem(31L, route, task2, 2);
        GameTeamSession session = makeSession(100L, game, team, route, item1, task1);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(captainMs));
        when(gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc(10L))
                .thenReturn(Collections.emptyList());
        when(gameTeamSessionRepository.findByTeamIdAndStatus(10L, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(session));
        when(teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(20L))
                .thenReturn(List.of(item1, item2));
        when(gameTeamSessionRepository.save(any(GameTeamSession.class))).thenReturn(session);

        SubmitTaskKeyResponse resp = gameService.submitTaskKey("cap@test.com",
                new SubmitTaskKeyRequest("KEY1"));

        assertFalse(resp.gameSessionFinished());
        assertEquals(6L, resp.nextTaskId());
    }

    @Test // №6 — Неверный ключ → BadRequestException
    void submitTaskKey_wrongKey_throwsBadRequest() {
        User captain = makeUser(2L, "cap@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMs = makeMembership(1L, team, captain,
                TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameTask task = makeTask(5L, game, "CORRECT", 60);
        TeamGameRoute route = makeRoute(20L, game);
        TeamGameRouteItem routeItem = makeRouteItem(30L, route, task, 1);
        GameTeamSession session = makeSession(100L, game, team, route, routeItem, task);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(captainMs));
        when(gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc(10L))
                .thenReturn(Collections.emptyList());
        when(gameTeamSessionRepository.findByTeamIdAndStatus(10L, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(Optional.of(session));

        assertThrows(BadRequestException.class,
                () -> gameService.submitTaskKey("cap@test.com",
                        new SubmitTaskKeyRequest("WRONG")));
    }

    // ─── getTeamRegistrations ─────────────────────────────────────────────────

    @Test // №7 — Успешное получение списка заявок команды
    void getTeamRegistrations_success_returnsList() {
        User participant = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, participant);
        TeamMembership membership = makeMembership(1L, team, participant,
                TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameRegistration reg = makeApprovedRegistration(5L, game, team);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(reg));
        stubInProgressSessions(GAME_ID);

        List<TeamGameRegistrationResponse> result =
                gameService.getTeamRegistrations("user@test.com");

        assertEquals(1, result.size());
    }

    @Test // №8 — Нет активной команды → NotFoundException
    void getTeamRegistrations_noActiveTeam_throwsNotFoundException() {
        User participant = makeUser(2L, "user@test.com", Role.PARTICIPANT);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> gameService.getTeamRegistrations("user@test.com"));
    }

    // ─── getTeamChatMessages ──────────────────────────────────────────────────

    @Test // №9 — Успешное получение сообщений командного чата
    void getTeamChatMessages_success_returnsMessages() {
        User participant = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, participant);
        TeamMembership membership = makeMembership(1L, team, participant,
                TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameRegistration reg = makeApprovedRegistration(5L, game, team);
        GameChatMessage msg = new GameChatMessage();
        msg.setId(100L);
        msg.setGame(game);
        msg.setTeam(team);
        msg.setSender(participant);
        msg.setChannel(GameChatChannel.TEAM);
        msg.setText("Hello!");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.of(reg));
        stubInProgressSessions(GAME_ID);
        when(gameChatMessageRepository
                .findAllByGameIdAndTeamIdAndChannelOrderByCreatedAtAsc(GAME_ID, 10L, GameChatChannel.TEAM))
                .thenReturn(List.of(msg));

        List<GameChatMessageResponse> result =
                gameService.getTeamChatMessages("user@test.com", GAME_ID);

        assertEquals(1, result.size());
        assertEquals("Hello!", result.get(0).text());
    }

    @Test // №10 — Команда не подтверждена → BadRequestException
    void getTeamChatMessages_notApproved_throwsBadRequest() {
        User participant = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, participant);
        TeamMembership membership = makeMembership(1L, team, participant,
                TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameRegistration pending = new GameRegistration();
        pending.setId(5L);
        pending.setGame(game);
        pending.setTeam(team);
        pending.setStatus(GameRegistrationStatus.PENDING);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.of(pending));

        assertThrows(BadRequestException.class,
                () -> gameService.getTeamChatMessages("user@test.com", GAME_ID));
    }

    // ─── sendTeamChatMessage ──────────────────────────────────────────────────

    @Test // №11 — Успешная отправка сообщения в командный чат
    void sendTeamChatMessage_success_savesAndReturnsMessage() {
        User participant = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, participant);
        TeamMembership membership = makeMembership(1L, team, participant,
                TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameRegistration reg = makeApprovedRegistration(5L, game, team);
        GameChatMessage saved = new GameChatMessage();
        saved.setId(100L);
        saved.setGame(game);
        saved.setTeam(team);
        saved.setSender(participant);
        saved.setChannel(GameChatChannel.TEAM);
        saved.setText("Hello!");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.of(reg));
        stubInProgressSessions(GAME_ID);
        when(gameChatMessageRepository.save(any(GameChatMessage.class))).thenReturn(saved);

        GameChatMessageResponse resp = gameService.sendTeamChatMessage(
                "user@test.com", GAME_ID, new CreateGameChatMessageRequest("Hello!"));

        assertEquals("Hello!", resp.text());
        verify(gameChatMessageRepository).save(any(GameChatMessage.class));
    }

    @Test // №12 — Пустой текст → BadRequestException (handled by sendChatMessage, not sendTeamChatMessage)
        // sendTeamChatMessage не вызывает sendChatMessage — у него своя логика без проверки длины текста,
        // поэтому проверяем что trim().isBlank → нет исключения от этого метода (blank обрабатывается в sendChatMessage).
        // Тест: команда не подтверждена → BadRequestException
    void sendTeamChatMessage_teamNotApproved_throwsBadRequest() {
        User participant = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, participant);
        TeamMembership membership = makeMembership(1L, team, participant,
                TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameRegistration pending = new GameRegistration();
        pending.setId(5L);
        pending.setGame(game);
        pending.setTeam(team);
        pending.setStatus(GameRegistrationStatus.PENDING);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.of(pending));

        assertThrows(BadRequestException.class,
                () -> gameService.sendTeamChatMessage("user@test.com", GAME_ID,
                        new CreateGameChatMessageRequest("Hello!")));
    }

    // ─── getCaptainOrganizerChatMessagesForCaptain ────────────────────────────

    @Test // №13 — Капитан получает сообщения чата капитан-организатор
    void getCaptainOrganizerChatMessagesForCaptain_success_returnsMessages() {
        User captain = makeUser(2L, "cap@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMs = makeMembership(1L, team, captain,
                TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameRegistration reg = makeApprovedRegistration(5L, game, team);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(captainMs));
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.of(reg));
        stubInProgressSessions(GAME_ID);
        when(gameChatMessageRepository
                .findAllByGameIdAndTeamIdAndChannelOrderByCreatedAtAsc(GAME_ID, 10L, GameChatChannel.CAPTAIN_ORGANIZER))
                .thenReturn(Collections.emptyList());

        List<GameChatMessageResponse> result =
                gameService.getCaptainOrganizerChatMessagesForCaptain("cap@test.com", GAME_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test // №14 — Пользователь не капитан → BadRequestException
    void getCaptainOrganizerChatMessagesForCaptain_notCaptain_throwsBadRequest() {
        User member = makeUser(2L, "member@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, member);
        TeamMembership memberMs = makeMembership(1L, team, member,
                TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameRegistration reg = makeApprovedRegistration(5L, game, team);

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(memberMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(memberMs));

        assertThrows(BadRequestException.class,
                () -> gameService.getCaptainOrganizerChatMessagesForCaptain("member@test.com", GAME_ID));
    }

    // ─── sendCaptainOrganizerChatMessageForCaptain ────────────────────────────

    @Test // №15 — Капитан успешно отправляет сообщение организатору
    void sendCaptainOrganizerChatMessageForCaptain_success_savesMessage() {
        User captain = makeUser(2L, "cap@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, captain);
        TeamMembership captainMs = makeMembership(1L, team, captain,
                TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameRegistration reg = makeApprovedRegistration(5L, game, team);
        GameChatMessage saved = new GameChatMessage();
        saved.setId(100L);
        saved.setGame(game);
        saved.setTeam(team);
        saved.setSender(captain);
        saved.setChannel(GameChatChannel.CAPTAIN_ORGANIZER);
        saved.setText("Need help");

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(captainMs));
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.of(reg));
        stubInProgressSessions(GAME_ID);
        when(gameChatMessageRepository.save(any(GameChatMessage.class))).thenReturn(saved);

        GameChatMessageResponse resp = gameService.sendCaptainOrganizerChatMessageForCaptain(
                "cap@test.com", GAME_ID, new CreateGameChatMessageRequest("Need help"));

        assertEquals("Need help", resp.text());
        verify(gameChatMessageRepository).save(any(GameChatMessage.class));
    }

    // ─── getCaptainOrganizerChatMessagesForOrganizer ──────────────────────────

    @Test // №16 — Организатор получает чат с командой
    void getCaptainOrganizerChatMessagesForOrganizer_success_returnsMessages() {
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        User captain = makeUser(2L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameRegistration reg = makeApprovedRegistration(5L, game, team);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubInProgressSessions(GAME_ID);
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.of(reg));
        when(gameChatMessageRepository
                .findAllByGameIdAndTeamIdAndChannelOrderByCreatedAtAsc(GAME_ID, 10L, GameChatChannel.CAPTAIN_ORGANIZER))
                .thenReturn(Collections.emptyList());

        List<GameChatMessageResponse> result =
                gameService.getCaptainOrganizerChatMessagesForOrganizer("org@test.com", GAME_ID, 10L);

        assertNotNull(result);
    }

    // ─── sendCaptainOrganizerChatMessageForOrganizer ──────────────────────────

    @Test // №17 — Организатор успешно отправляет сообщение капитану
    void sendCaptainOrganizerChatMessageForOrganizer_success_savesMessage() {
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        User captain = makeUser(2L, "cap@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, captain);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameRegistration reg = makeApprovedRegistration(5L, game, team);
        GameChatMessage saved = new GameChatMessage();
        saved.setId(100L);
        saved.setGame(game);
        saved.setTeam(team);
        saved.setSender(organizer);
        saved.setChannel(GameChatChannel.CAPTAIN_ORGANIZER);
        saved.setText("Good luck!");

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubInProgressSessions(GAME_ID);
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.of(reg));
        when(gameChatMessageRepository.save(any(GameChatMessage.class))).thenReturn(saved);

        GameChatMessageResponse resp = gameService.sendCaptainOrganizerChatMessageForOrganizer(
                "org@test.com", GAME_ID, 10L, new CreateGameChatMessageRequest("Good luck!"));

        assertEquals("Good luck!", resp.text());
        verify(gameChatMessageRepository).save(any(GameChatMessage.class));
    }

    // ─── getMyTeamProgress ────────────────────────────────────────────────────

    @Test // №18 — Успешное получение прогресса команды
    void getMyTeamProgress_success_returnsProgress() {
        User participant = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        User organizer = makeUser(1L, "org@test.com", Role.ORGANIZER);
        Team team = makeTeam(10L, participant);
        TeamMembership membership = makeMembership(1L, team, participant,
                TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);
        Game game = makeInProgressGame(GAME_ID, organizer);
        GameTask task = makeTask(5L, game, "ANSWER", 60);
        TeamGameRoute route = makeRoute(20L, game);
        TeamGameRouteItem routeItem = makeRouteItem(30L, route, task, 1);
        GameTeamSession session = makeSession(100L, game, team, route, routeItem, task);
        session.setStatus(GameTeamSessionStatus.FINISHED);
        session.setFinishedAt(Instant.now().minus(5, ChronoUnit.MINUTES));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc(10L))
                .thenReturn(Collections.emptyList());
        when(gameTeamSessionRepository.findTopByTeamIdOrderByStartedAtDesc(10L))
                .thenReturn(Optional.of(session));
        when(gameTeamSessionRepository.findAllByGameId(GAME_ID))
                .thenReturn(new ArrayList<>(List.of(session)));
        // session is FINISHED → synchronizeSessionWithTimeout won't loop (status != IN_PROGRESS)
        when(teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(20L))
                .thenReturn(List.of(routeItem));

        GameTeamProgressResponse resp = gameService.getMyTeamProgress("user@test.com");

        assertEquals(GAME_ID, resp.gameId());
        assertEquals(10L, resp.teamId());
        assertEquals(1, resp.currentPlace());
    }

    @Test // №19 — Нет игровой сессии → NotFoundException
    void getMyTeamProgress_noSession_throwsNotFoundException() {
        User participant = makeUser(2L, "user@test.com", Role.PARTICIPANT);
        Team team = makeTeam(10L, participant);
        TeamMembership membership = makeMembership(1L, team, participant,
                TeamMembershipRole.MEMBER, TeamMembershipStatus.ACTIVE);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership));
        when(gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc(10L))
                .thenReturn(Collections.emptyList());
        when(gameTeamSessionRepository.findTopByTeamIdOrderByStartedAtDesc(10L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> gameService.getMyTeamProgress("user@test.com"));
    }
}