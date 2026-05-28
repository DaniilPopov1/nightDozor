package com.example.server.unit.game;

import com.example.server.auth.entity.Role;
import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import com.example.server.common.exception.BadRequestException;
import com.example.server.common.exception.ConflictException;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceLifecycleTest {

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

    private User makeOrganizer(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(Role.ORGANIZER);
        return u;
    }

    private User makeParticipant(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setRole(Role.PARTICIPANT);
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

    /**
     * DRAFT: registrationStartsAt в будущем → computePlannedStatus = DRAFT
     * (при synchronize вычисленный статус совпадает с текущим → save не вызывается)
     */
    private Game makeDraftGame(Long id, User organizer) {
        Game g = new Game();
        g.setId(id);
        g.setTitle("Test Game");
        g.setDescription("Description");
        g.setCity("Moscow");
        g.setStatus(GameStatus.DRAFT);
        g.setMinTeamSize(2);
        g.setMaxTeamSize(4);
        g.setTaskFailurePenaltyMinutes(5);
        g.setRouteSlotsCount(2);
        g.setRegistrationStartsAt(Instant.now().plus(24, ChronoUnit.HOURS));
        g.setRegistrationEndsAt(Instant.now().plus(48, ChronoUnit.HOURS));
        g.setStartsAt(Instant.now().plus(72, ChronoUnit.HOURS));
        g.setOrganizer(organizer);
        return g;
    }

    /**
     * REGISTRATION_OPEN: regStart в прошлом, regEnd и startsAt в будущем
     * → computePlannedStatus = REGISTRATION_OPEN → save из lifecycle не вызывается
     */
    private Game makeRegOpenGame(Long id, User organizer) {
        Game g = new Game();
        g.setId(id);
        g.setTitle("Test Game");
        g.setDescription("Description");
        g.setCity("Moscow");
        g.setStatus(GameStatus.REGISTRATION_OPEN);
        g.setMinTeamSize(2);
        g.setMaxTeamSize(4);
        g.setTaskFailurePenaltyMinutes(5);
        g.setRouteSlotsCount(2);
        g.setRegistrationStartsAt(Instant.now().minus(1, ChronoUnit.HOURS));
        g.setRegistrationEndsAt(Instant.now().plus(24, ChronoUnit.HOURS));
        g.setStartsAt(Instant.now().plus(48, ChronoUnit.HOURS));
        g.setOrganizer(organizer);
        return g;
    }

    private GameTask makeTask(Long id, Game game, Integer orderIndex) {
        GameTask t = new GameTask();
        t.setId(id);
        t.setGame(game);
        t.setTitle("Task " + orderIndex);
        t.setRiddleText("Riddle");
        t.setAnswerKey("KEY");
        t.setOrderIndex(orderIndex);
        t.setTimeLimitMinutes(30);
        t.setFailurePenaltyMinutes(5);
        return t;
    }

    private GameTaskHint makeHint(Long id, GameTask task, Integer orderIndex) {
        GameTaskHint h = new GameTaskHint();
        h.setId(id);
        h.setTask(task);
        h.setText("Hint text");
        h.setOrderIndex(orderIndex);
        h.setDelayMinutesFromPreviousHint(10);
        return h;
    }

    private TeamGameRoute makeRoute(Long id, Game game, Integer slot) {
        TeamGameRoute r = new TeamGameRoute();
        r.setId(id);
        r.setGame(game);
        r.setSlotNumber(slot);
        r.setName("Route " + slot);
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

    private GameRegistration makeRegistration(Long id, Game game, Team team, GameRegistrationStatus status) {
        GameRegistration r = new GameRegistration();
        r.setId(id);
        r.setGame(game);
        r.setTeam(team);
        r.setStatus(status);
        return r;
    }

    /** Нет активных/существующих сессий → synchronizeGameLifecycle идёт по пути computePlannedStatus */
    private void stubNoSessions(Long gameId) {
        when(gameTeamSessionRepository.existsByGameIdAndStatus(gameId, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(false);
        when(gameTeamSessionRepository.existsByGameId(gameId)).thenReturn(false);
    }

    // ─── createGame ───────────────────────────────────────────────────────────

    @Test // №1 — Организатор успешно создаёт игру
    void createGame_success_savesAndReturnsResponse() {
        User organizer = makeOrganizer(1L, "org@test.com");
        CreateGameRequest req = new CreateGameRequest(
                "Quest", "Desc", "Moscow", 2, 4, 5, 2,
                Instant.now().plus(24, ChronoUnit.HOURS),
                Instant.now().plus(48, ChronoUnit.HOURS),
                Instant.now().plus(72, ChronoUnit.HOURS));
        Game saved = makeDraftGame(10L, organizer);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.save(any(Game.class))).thenReturn(saved);

        GameResponse resp = gameService.createGame("org@test.com", req);

        assertEquals(10L, resp.id());
        verify(gameRepository).save(any(Game.class));
    }

    @Test // №2 — Пользователь не организатор → BadRequestException
    void createGame_notOrganizer_throwsBadRequest() {
        User participant = makeParticipant(2L, "user@test.com");
        CreateGameRequest req = new CreateGameRequest(
                "Quest", "Desc", "Moscow", 2, 4, 5, 2,
                null, null, Instant.now().plus(72, ChronoUnit.HOURS));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(participant));

        assertThrows(BadRequestException.class,
                () -> gameService.createGame("user@test.com", req));
    }

    @Test // №3 — minTeamSize > maxTeamSize → BadRequestException
    void createGame_minSizeGreaterThanMax_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        CreateGameRequest req = new CreateGameRequest(
                "Quest", "Desc", "Moscow", 5, 2, 5, 2,
                null, null, Instant.now().plus(72, ChronoUnit.HOURS));

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));

        assertThrows(BadRequestException.class,
                () -> gameService.createGame("org@test.com", req));
    }

    // ─── updateGame ───────────────────────────────────────────────────────────

    @Test // №4 — Успешное обновление DRAFT игры
    void updateGame_success_updatesAndReturnsGame() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        UpdateGameRequest req = new UpdateGameRequest(
                "New Title", "New Desc", "SPb", 2, 4, 5, 2,
                null, null, Instant.now().plus(72, ChronoUnit.HOURS));
        Game saved = makeDraftGame(GAME_ID, organizer);
        saved.setTitle("New Title");

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(teamGameRouteRepository.findAllByGameIdOrderBySlotNumberAsc(GAME_ID))
                .thenReturn(Collections.emptyList());
        when(gameRegistrationRepository.findAllByGameIdAndStatusOrderByCreatedAtDesc(
                GAME_ID, GameRegistrationStatus.APPROVED)).thenReturn(Collections.emptyList());
        when(gameRepository.save(any(Game.class))).thenReturn(saved);

        GameResponse resp = gameService.updateGame("org@test.com", GAME_ID, req);

        assertEquals("New Title", resp.title());
    }

    @Test // №5 — Игра IN_PROGRESS → validateGameEditable → BadRequestException
    void updateGame_gameInProgress_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        game.setStatus(GameStatus.IN_PROGRESS);
        UpdateGameRequest req = new UpdateGameRequest(
                "New", "Desc", "Moscow", 2, 4, 5, 2,
                null, null, Instant.now().plus(72, ChronoUnit.HOURS));

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));

        assertThrows(BadRequestException.class,
                () -> gameService.updateGame("org@test.com", GAME_ID, req));
    }

    @Test // №6 — Игра не найдена → NotFoundException
    void updateGame_gameNotFound_throwsNotFoundException() {
        User organizer = makeOrganizer(1L, "org@test.com");
        UpdateGameRequest req = new UpdateGameRequest(
                "New", "Desc", "Moscow", 2, 4, 5, 2,
                null, null, Instant.now().plus(72, ChronoUnit.HOURS));

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> gameService.updateGame("org@test.com", GAME_ID, req));
    }

    // ─── cancelGame ───────────────────────────────────────────────────────────

    @Test // №7 — Успешная отмена REGISTRATION_OPEN игры
    void cancelGame_success_setsStatusCanceled() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeRegOpenGame(GAME_ID, organizer);
        Game saved = makeRegOpenGame(GAME_ID, organizer);
        saved.setStatus(GameStatus.CANCELED);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);
        when(gameRepository.save(any(Game.class))).thenReturn(saved);

        GameResponse resp = gameService.cancelGame("org@test.com", GAME_ID);

        assertEquals(GameStatus.CANCELED, resp.status());
    }

    @Test // №8 — Игра уже FINISHED → BadRequestException
    void cancelGame_alreadyFinished_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        game.setStatus(GameStatus.FINISHED);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));

        assertThrows(BadRequestException.class,
                () -> gameService.cancelGame("org@test.com", GAME_ID));
    }

    @Test // №9 — Игра уже CANCELED → BadRequestException
    void cancelGame_alreadyCanceled_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        game.setStatus(GameStatus.CANCELED);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));

        assertThrows(BadRequestException.class,
                () -> gameService.cancelGame("org@test.com", GAME_ID));
    }

    // ─── startGame ────────────────────────────────────────────────────────────

    @Test // №10 — Игра IN_PROGRESS (есть сессии) → успешный старт
    void startGame_inProgress_returnsStartResponse() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        game.setStatus(GameStatus.IN_PROGRESS);

        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        GameTeamSession session = new GameTeamSession();
        session.setId(1L);
        session.setGame(game);
        session.setTeam(team);
        session.setStatus(GameTeamSessionStatus.IN_PROGRESS);
        session.setStartedAt(Instant.now().minus(10, ChronoUnit.MINUTES));

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTeamSessionRepository.existsByGameIdAndStatus(GAME_ID, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(true);
        when(gameTeamSessionRepository.findAllByGameId(GAME_ID)).thenReturn(List.of(session));

        GameStartResponse resp = gameService.startGame("org@test.com", GAME_ID);

        assertEquals(GAME_ID, resp.gameId());
        assertEquals(1, resp.startedSessionsCount());
    }

    @Test // №11 — Игра ещё не запущена (DRAFT, нет сессий) → BadRequestException
    void startGame_notYetStartable_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTeamSessionRepository.existsByGameIdAndStatus(GAME_ID, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(false);
        when(gameTeamSessionRepository.existsByGameId(GAME_ID)).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> gameService.startGame("org@test.com", GAME_ID));
    }

    // ─── submitGameRegistration ───────────────────────────────────────────────

    @Test // №12 — Капитан успешно подаёт заявку
    void submitGameRegistration_success_savesPendingRegistration() {
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        TeamMembership captainMs = makeMembership(1L, team, captain,
                TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        Game game = makeRegOpenGame(GAME_ID, makeOrganizer(1L, "org@test.com"));
        GameRegistration saved = makeRegistration(5L, game, team, GameRegistrationStatus.PENDING);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(captainMs));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);
        when(teamMembershipRepository.countByTeamIdAndStatus(10L, TeamMembershipStatus.ACTIVE))
                .thenReturn(3L);
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.empty());
        when(gameRegistrationRepository.save(any(GameRegistration.class))).thenReturn(saved);

        GameRegistrationResponse resp = gameService.submitGameRegistration("cap@test.com", GAME_ID);

        assertEquals(GameRegistrationStatus.PENDING, resp.status());
        verify(gameRegistrationRepository).save(any(GameRegistration.class));
    }

    @Test // №13 — Регистрация закрыта → BadRequestException
    void submitGameRegistration_notRegistrationOpen_throwsBadRequest() {
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        TeamMembership captainMs = makeMembership(1L, team, captain,
                TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        Game game = makeDraftGame(GAME_ID, makeOrganizer(1L, "org@test.com"));
        game.setStatus(GameStatus.FINISHED); // FINISHED → synchronize returns immediately

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(captainMs));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));

        assertThrows(BadRequestException.class,
                () -> gameService.submitGameRegistration("cap@test.com", GAME_ID));
    }

    @Test // №14 — Размер команды вне допустимого диапазона → BadRequestException
    void submitGameRegistration_teamSizeMismatch_throwsBadRequest() {
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        TeamMembership captainMs = makeMembership(1L, team, captain,
                TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        Game game = makeRegOpenGame(GAME_ID, makeOrganizer(1L, "org@test.com"));
        game.setMinTeamSize(3);
        game.setMaxTeamSize(5);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(captainMs));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);
        when(teamMembershipRepository.countByTeamIdAndStatus(10L, TeamMembershipStatus.ACTIVE))
                .thenReturn(1L); // 1 < minTeamSize=3

        assertThrows(BadRequestException.class,
                () -> gameService.submitGameRegistration("cap@test.com", GAME_ID));
    }

    @Test // №15 — Заявка уже существует (PENDING) → ConflictException
    void submitGameRegistration_alreadyPending_throwsConflict() {
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        TeamMembership captainMs = makeMembership(1L, team, captain,
                TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        Game game = makeRegOpenGame(GAME_ID, makeOrganizer(1L, "org@test.com"));
        GameRegistration existing = makeRegistration(5L, game, team, GameRegistrationStatus.PENDING);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(captainMs));
        when(gameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);
        when(teamMembershipRepository.countByTeamIdAndStatus(10L, TeamMembershipStatus.ACTIVE))
                .thenReturn(3L);
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class,
                () -> gameService.submitGameRegistration("cap@test.com", GAME_ID));
    }

    // ─── cancelGameRegistration ───────────────────────────────────────────────

    @Test // №16 — Успешная отмена PENDING заявки
    void cancelGameRegistration_success_setsCanceled() {
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        TeamMembership captainMs = makeMembership(1L, team, captain,
                TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        Game game = makeRegOpenGame(GAME_ID, makeOrganizer(1L, "org@test.com"));
        GameRegistration reg = makeRegistration(5L, game, team, GameRegistrationStatus.PENDING);
        GameRegistration saved = makeRegistration(5L, game, team, GameRegistrationStatus.CANCELED);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(captainMs));
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.of(reg));
        when(gameRegistrationRepository.save(any(GameRegistration.class))).thenReturn(saved);

        GameRegistrationResponse resp = gameService.cancelGameRegistration("cap@test.com", GAME_ID);

        assertEquals(GameRegistrationStatus.CANCELED, resp.status());
    }

    @Test // №17 — Заявка не в статусе PENDING → BadRequestException
    void cancelGameRegistration_notPending_throwsBadRequest() {
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        TeamMembership captainMs = makeMembership(1L, team, captain,
                TeamMembershipRole.CAPTAIN, TeamMembershipStatus.ACTIVE);
        Game game = makeRegOpenGame(GAME_ID, makeOrganizer(1L, "org@test.com"));
        GameRegistration reg = makeRegistration(5L, game, team, GameRegistrationStatus.APPROVED);

        when(userRepository.findByEmail("cap@test.com")).thenReturn(Optional.of(captain));
        when(teamMembershipRepository.findByUserIdAndStatus(2L, TeamMembershipStatus.ACTIVE))
                .thenReturn(Optional.of(captainMs));
        when(teamMembershipRepository.findByTeamIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(captainMs));
        when(gameRegistrationRepository.findByGameIdAndTeamId(GAME_ID, 10L))
                .thenReturn(Optional.of(reg));

        assertThrows(BadRequestException.class,
                () -> gameService.cancelGameRegistration("cap@test.com", GAME_ID));
    }

    // ─── approveRegistration ─────────────────────────────────────────────────

    @Test // №18 — Успешное одобрение заявки (назначается случайный маршрут)
    void approveRegistration_success_assignsRouteAndSetsApproved() {
        User organizer = makeOrganizer(1L, "org@test.com");
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        Game game = makeRegOpenGame(GAME_ID, organizer);
        GameRegistration reg = makeRegistration(5L, game, team, GameRegistrationStatus.PENDING);
        TeamGameRoute route = makeRoute(20L, game, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);
        when(gameRegistrationRepository.findByIdAndGameId(5L, GAME_ID)).thenReturn(Optional.of(reg));
        when(teamGameRouteRepository.findAllByGameIdAndAssignedTeamIsNullOrderBySlotNumberAsc(GAME_ID))
                .thenReturn(List.of(route));
        when(teamGameRouteRepository.save(any(TeamGameRoute.class))).thenReturn(route);
        GameRegistration saved = makeRegistration(5L, game, team, GameRegistrationStatus.APPROVED);
        when(gameRegistrationRepository.save(any(GameRegistration.class))).thenReturn(saved);

        GameRegistrationResponse resp = gameService.approveRegistration("org@test.com", GAME_ID, 5L);

        assertEquals(GameRegistrationStatus.APPROVED, resp.status());
        verify(teamGameRouteRepository).save(any(TeamGameRoute.class));
    }

    @Test // №19 — Нет свободных маршрутов → BadRequestException
    void approveRegistration_noFreeRoutes_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        Game game = makeRegOpenGame(GAME_ID, organizer);
        GameRegistration reg = makeRegistration(5L, game, team, GameRegistrationStatus.PENDING);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);
        when(gameRegistrationRepository.findByIdAndGameId(5L, GAME_ID)).thenReturn(Optional.of(reg));
        when(teamGameRouteRepository.findAllByGameIdAndAssignedTeamIsNullOrderBySlotNumberAsc(GAME_ID))
                .thenReturn(Collections.emptyList());

        assertThrows(BadRequestException.class,
                () -> gameService.approveRegistration("org@test.com", GAME_ID, 5L));
    }

    @Test // №20 — Игра уже IN_PROGRESS → нельзя одобрять → BadRequestException
    void approveRegistration_gameInProgress_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        game.setStatus(GameStatus.IN_PROGRESS);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        // IN_PROGRESS + existsByGameIdAndStatus=true → synchronize: status stays IN_PROGRESS
        when(gameTeamSessionRepository.existsByGameIdAndStatus(GAME_ID, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> gameService.approveRegistration("org@test.com", GAME_ID, 5L));
    }

    // ─── rejectRegistration ───────────────────────────────────────────────────

    @Test // №21 — Успешное отклонение PENDING заявки
    void rejectRegistration_success_setsRejected() {
        User organizer = makeOrganizer(1L, "org@test.com");
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        Game game = makeRegOpenGame(GAME_ID, organizer);
        GameRegistration reg = makeRegistration(5L, game, team, GameRegistrationStatus.PENDING);
        GameRegistration saved = makeRegistration(5L, game, team, GameRegistrationStatus.REJECTED);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);
        when(gameRegistrationRepository.findByIdAndGameId(5L, GAME_ID)).thenReturn(Optional.of(reg));
        when(gameRegistrationRepository.save(any(GameRegistration.class))).thenReturn(saved);

        GameRegistrationResponse resp = gameService.rejectRegistration("org@test.com", GAME_ID, 5L);

        assertEquals(GameRegistrationStatus.REJECTED, resp.status());
    }

    @Test // №22 — Заявка не PENDING → BadRequestException
    void rejectRegistration_notPending_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        Game game = makeRegOpenGame(GAME_ID, organizer);
        GameRegistration reg = makeRegistration(5L, game, team, GameRegistrationStatus.APPROVED);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);
        when(gameRegistrationRepository.findByIdAndGameId(5L, GAME_ID)).thenReturn(Optional.of(reg));

        assertThrows(BadRequestException.class,
                () -> gameService.rejectRegistration("org@test.com", GAME_ID, 5L));
    }

    // ─── createTask ───────────────────────────────────────────────────────────

    @Test // №23 — Успешное создание задания
    void createTask_success_savesAndReturnsTask() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        CreateGameTaskRequest req = new CreateGameTaskRequest(
                "Task 1", "Riddle", "ANSWER", 1, 30, 5);
        GameTask saved = makeTask(10L, game, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findAllByGameIdOrderByOrderIndexAsc(GAME_ID))
                .thenReturn(Collections.emptyList());
        when(gameTaskRepository.save(any(GameTask.class))).thenReturn(saved);

        GameTaskResponse resp = gameService.createTask("org@test.com", GAME_ID, req);

        assertEquals(10L, resp.id());
        assertEquals(1, resp.orderIndex());
        verify(gameTaskRepository).save(any(GameTask.class));
    }

    @Test // №24 — Задание с таким orderIndex уже существует → ConflictException
    void createTask_orderIndexTaken_throwsConflict() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        GameTask existing = makeTask(5L, game, 1);
        CreateGameTaskRequest req = new CreateGameTaskRequest(
                "Task 1", "Riddle", "ANSWER", 1, 30, 5);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findAllByGameIdOrderByOrderIndexAsc(GAME_ID))
                .thenReturn(List.of(existing));

        assertThrows(ConflictException.class,
                () -> gameService.createTask("org@test.com", GAME_ID, req));
    }

    // ─── updateTask ───────────────────────────────────────────────────────────

    @Test // №25 — Успешное обновление задания
    void updateTask_success_updatesAndReturnsTask() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        GameTask task = makeTask(10L, game, 1);
        UpdateGameTaskRequest req = new UpdateGameTaskRequest(
                "Updated", "New riddle", "NEWKEY", 1, 45, 10);
        GameTask saved = makeTask(10L, game, 1);
        saved.setTitle("Updated");

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(gameTaskRepository.findAllByGameIdOrderByOrderIndexAsc(GAME_ID))
                .thenReturn(List.of(task));
        when(gameTaskRepository.save(any(GameTask.class))).thenReturn(saved);

        GameTaskResponse resp = gameService.updateTask("org@test.com", GAME_ID, 10L, req);

        assertEquals("Updated", resp.title());
    }

    @Test // №26 — Другое задание занимает этот orderIndex → ConflictException
    void updateTask_orderIndexTakenByOtherTask_throwsConflict() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        GameTask task = makeTask(10L, game, 2);
        GameTask other = makeTask(11L, game, 1);
        UpdateGameTaskRequest req = new UpdateGameTaskRequest(
                "Updated", "Riddle", "KEY", 1, 30, 5); // orderIndex=1 уже у другого задания

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(gameTaskRepository.findAllByGameIdOrderByOrderIndexAsc(GAME_ID))
                .thenReturn(List.of(other, task));

        assertThrows(ConflictException.class,
                () -> gameService.updateTask("org@test.com", GAME_ID, 10L, req));
    }

    // ─── deleteTask ───────────────────────────────────────────────────────────

    @Test // №27 — Успешное удаление задания
    void deleteTask_success_deletesTask() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        GameTask task = makeTask(10L, game, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(teamGameRouteItemRepository.existsByTaskIdAndRouteGameId(10L, GAME_ID))
                .thenReturn(false);

        gameService.deleteTask("org@test.com", GAME_ID, 10L);

        verify(gameTaskRepository).delete(task);
    }

    @Test // №28 — Задание используется в маршруте → ConflictException
    void deleteTask_usedInRoute_throwsConflict() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        GameTask task = makeTask(10L, game, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(teamGameRouteItemRepository.existsByTaskIdAndRouteGameId(10L, GAME_ID))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> gameService.deleteTask("org@test.com", GAME_ID, 10L));
    }

    // ─── addTaskHint ──────────────────────────────────────────────────────────

    @Test // №29 — Успешное добавление подсказки
    void addTaskHint_success_savesAndReturnsHint() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        GameTask task = makeTask(10L, game, 1);
        CreateGameTaskHintRequest req = new CreateGameTaskHintRequest("Hint text", 1, 15);
        GameTaskHint saved = makeHint(20L, task, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(gameTaskHintRepository.findAllByTaskIdOrderByOrderIndexAsc(10L))
                .thenReturn(Collections.emptyList());
        when(gameTaskHintRepository.save(any(GameTaskHint.class))).thenReturn(saved);

        var resp = gameService.addTaskHint("org@test.com", GAME_ID, 10L, req);

        assertEquals(20L, resp.id());
    }

    @Test // №30 — Уже 2 подсказки → BadRequestException
    void addTaskHint_maxHintsReached_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        GameTask task = makeTask(10L, game, 1);
        GameTaskHint hint1 = makeHint(20L, task, 1);
        GameTaskHint hint2 = makeHint(21L, task, 2);
        CreateGameTaskHintRequest req = new CreateGameTaskHintRequest("Hint 3", 1, 10);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(gameTaskHintRepository.findAllByTaskIdOrderByOrderIndexAsc(10L))
                .thenReturn(List.of(hint1, hint2));

        assertThrows(BadRequestException.class,
                () -> gameService.addTaskHint("org@test.com", GAME_ID, 10L, req));
    }

    @Test // №31 — Задание не найдено → NotFoundException
    void addTaskHint_taskNotFound_throwsNotFoundException() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> gameService.addTaskHint("org@test.com", GAME_ID, 10L,
                        new CreateGameTaskHintRequest("text", 1, 0)));
    }

    // ─── updateTaskHint ───────────────────────────────────────────────────────

    @Test // №32 — Успешное обновление подсказки
    void updateTaskHint_success_updatesAndReturnsHint() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        GameTask task = makeTask(10L, game, 1);
        GameTaskHint hint = makeHint(20L, task, 1);
        UpdateGameTaskHintRequest req = new UpdateGameTaskHintRequest("Updated hint", 1, 20);
        GameTaskHint saved = makeHint(20L, task, 1);
        saved.setText("Updated hint");

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(gameTaskHintRepository.findByIdAndTaskId(20L, 10L)).thenReturn(Optional.of(hint));
        when(gameTaskHintRepository.findAllByTaskIdOrderByOrderIndexAsc(10L))
                .thenReturn(List.of(hint));
        when(gameTaskHintRepository.save(any(GameTaskHint.class))).thenReturn(saved);

        var resp = gameService.updateTaskHint("org@test.com", GAME_ID, 10L, 20L, req);

        assertEquals("Updated hint", resp.text());
    }

    @Test // №33 — Подсказка не найдена → NotFoundException
    void updateTaskHint_hintNotFound_throwsNotFoundException() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        GameTask task = makeTask(10L, game, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(gameTaskHintRepository.findByIdAndTaskId(20L, 10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> gameService.updateTaskHint("org@test.com", GAME_ID, 10L, 20L,
                        new UpdateGameTaskHintRequest("text", 1, 0)));
    }

    // ─── deleteTaskHint ───────────────────────────────────────────────────────

    @Test // №34 — Успешное удаление подсказки
    void deleteTaskHint_success_deletesHint() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        GameTask task = makeTask(10L, game, 1);
        GameTaskHint hint = makeHint(20L, task, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(gameTaskHintRepository.findByIdAndTaskId(20L, 10L)).thenReturn(Optional.of(hint));

        gameService.deleteTaskHint("org@test.com", GAME_ID, 10L, 20L);

        verify(gameTaskHintRepository).delete(hint);
    }

    // ─── createRoute ──────────────────────────────────────────────────────────

    @Test // №35 — Успешное создание маршрута
    void createRoute_success_savesAndReturnsRoute() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        game.setRouteSlotsCount(2);
        CreateTeamGameRouteRequest req = new CreateTeamGameRouteRequest(1L, "Route A");
        TeamGameRoute saved = makeRoute(30L, game, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(teamGameRouteRepository.findByGameIdAndSlotNumber(GAME_ID, 1))
                .thenReturn(Optional.empty());
        when(teamGameRouteRepository.save(any(TeamGameRoute.class))).thenReturn(saved);

        TeamGameRouteResponse resp = gameService.createRoute("org@test.com", GAME_ID, req);

        assertEquals(30L, resp.id());
        assertEquals(1, resp.slotNumber());
    }

    @Test // №36 — Номер слота вне диапазона → BadRequestException
    void createRoute_slotOutOfRange_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        game.setRouteSlotsCount(2);
        CreateTeamGameRouteRequest req = new CreateTeamGameRouteRequest(5L, "Route X");

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));

        assertThrows(BadRequestException.class,
                () -> gameService.createRoute("org@test.com", GAME_ID, req));
    }

    @Test // №37 — Маршрут с таким слотом уже существует → ConflictException
    void createRoute_slotAlreadyTaken_throwsConflict() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        game.setRouteSlotsCount(2);
        TeamGameRoute existing = makeRoute(30L, game, 1);
        CreateTeamGameRouteRequest req = new CreateTeamGameRouteRequest(1L, "Route A");

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(teamGameRouteRepository.findByGameIdAndSlotNumber(GAME_ID, 1))
                .thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class,
                () -> gameService.createRoute("org@test.com", GAME_ID, req));
    }

    // ─── updateRoute ──────────────────────────────────────────────────────────

    @Test // №38 — Успешное обновление маршрута
    void updateRoute_success_updatesName() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        TeamGameRoute route = makeRoute(30L, game, 1);
        UpdateTeamGameRouteRequest req = new UpdateTeamGameRouteRequest("New Name");
        TeamGameRoute saved = makeRoute(30L, game, 1);
        saved.setName("New Name");

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(teamGameRouteRepository.findByIdAndGameId(30L, GAME_ID)).thenReturn(Optional.of(route));
        when(teamGameRouteRepository.save(any(TeamGameRoute.class))).thenReturn(saved);

        TeamGameRouteResponse resp = gameService.updateRoute("org@test.com", GAME_ID, 30L, req);

        assertEquals("New Name", resp.name());
    }

    // ─── deleteRoute ──────────────────────────────────────────────────────────

    @Test // №39 — Успешное удаление маршрута
    void deleteRoute_success_deletesRoute() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        TeamGameRoute route = makeRoute(30L, game, 1);
        route.setAssignedTeam(null);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(teamGameRouteRepository.findByIdAndGameId(30L, GAME_ID)).thenReturn(Optional.of(route));

        gameService.deleteRoute("org@test.com", GAME_ID, 30L);

        verify(teamGameRouteRepository).delete(route);
    }

    @Test // №40 — Маршрут назначен команде → ConflictException
    void deleteRoute_assignedToTeam_throwsConflict() {
        User organizer = makeOrganizer(1L, "org@test.com");
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        Game game = makeDraftGame(GAME_ID, organizer);
        TeamGameRoute route = makeRoute(30L, game, 1);
        route.setAssignedTeam(team);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(teamGameRouteRepository.findByIdAndGameId(30L, GAME_ID)).thenReturn(Optional.of(route));

        assertThrows(ConflictException.class,
                () -> gameService.deleteRoute("org@test.com", GAME_ID, 30L));
    }

    // ─── addTaskToRoute ───────────────────────────────────────────────────────

    @Test // №41 — Успешное добавление задания в маршрут
    void addTaskToRoute_success_savesItemAndReturnsRoute() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        TeamGameRoute route = makeRoute(30L, game, 1);
        GameTask task = makeTask(10L, game, 1);
        AddTaskToRouteRequest req = new AddTaskToRouteRequest(10L, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(teamGameRouteRepository.findByIdAndGameId(30L, GAME_ID)).thenReturn(Optional.of(route));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(30L))
                .thenReturn(Collections.emptyList());
        when(teamGameRouteItemRepository.existsByTaskIdAndRouteGameId(10L, GAME_ID))
                .thenReturn(false);
        when(teamGameRouteItemRepository.save(any(TeamGameRouteItem.class)))
                .thenReturn(makeRouteItem(50L, route, task, 1));

        TeamGameRouteResponse resp = gameService.addTaskToRoute("org@test.com", GAME_ID, 30L, req);

        assertEquals(30L, resp.id());
        verify(teamGameRouteItemRepository).save(any(TeamGameRouteItem.class));
    }

    @Test // №42 — Позиция в маршруте уже занята → ConflictException
    void addTaskToRoute_orderIndexTaken_throwsConflict() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        TeamGameRoute route = makeRoute(30L, game, 1);
        GameTask task = makeTask(10L, game, 1);
        GameTask existingTask = makeTask(11L, game, 2);
        TeamGameRouteItem existingItem = makeRouteItem(50L, route, existingTask, 1);
        AddTaskToRouteRequest req = new AddTaskToRouteRequest(10L, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(teamGameRouteRepository.findByIdAndGameId(30L, GAME_ID)).thenReturn(Optional.of(route));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(30L))
                .thenReturn(List.of(existingItem));

        assertThrows(ConflictException.class,
                () -> gameService.addTaskToRoute("org@test.com", GAME_ID, 30L, req));
    }

    @Test // №43 — Задание уже используется в другом маршруте → ConflictException
    void addTaskToRoute_taskUsedInAnotherRoute_throwsConflict() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        TeamGameRoute route = makeRoute(30L, game, 1);
        GameTask task = makeTask(10L, game, 1);
        AddTaskToRouteRequest req = new AddTaskToRouteRequest(10L, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(teamGameRouteRepository.findByIdAndGameId(30L, GAME_ID)).thenReturn(Optional.of(route));
        when(gameTaskRepository.findByIdAndGameId(10L, GAME_ID)).thenReturn(Optional.of(task));
        when(teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(30L))
                .thenReturn(Collections.emptyList());
        when(teamGameRouteItemRepository.existsByTaskIdAndRouteGameId(10L, GAME_ID))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> gameService.addTaskToRoute("org@test.com", GAME_ID, 30L, req));
    }

    // ─── removeTaskFromRoute ──────────────────────────────────────────────────

    @Test // №44 — Успешное удаление задания из маршрута
    void removeTaskFromRoute_success_deletesItem() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        TeamGameRoute route = makeRoute(30L, game, 1);
        GameTask task = makeTask(10L, game, 1);
        TeamGameRouteItem item = makeRouteItem(50L, route, task, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(teamGameRouteRepository.findByIdAndGameId(30L, GAME_ID)).thenReturn(Optional.of(route));
        when(teamGameRouteItemRepository.findByIdAndRouteId(50L, 30L)).thenReturn(Optional.of(item));

        gameService.removeTaskFromRoute("org@test.com", GAME_ID, 30L, 50L);

        verify(teamGameRouteItemRepository).delete(item);
    }

    @Test // №45 — Элемент маршрута не найден → NotFoundException
    void removeTaskFromRoute_itemNotFound_throwsNotFoundException() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        TeamGameRoute route = makeRoute(30L, game, 1);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(teamGameRouteRepository.findByIdAndGameId(30L, GAME_ID)).thenReturn(Optional.of(route));
        when(teamGameRouteItemRepository.findByIdAndRouteId(50L, 30L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> gameService.removeTaskFromRoute("org@test.com", GAME_ID, 30L, 50L));
    }

    // ─── getOrganizerGames ────────────────────────────────────────────────────

    @Test // №46 — Возвращает список игр организатора
    void getOrganizerGames_success_returnsList() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findAllByOrganizerIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(game));
        stubNoSessions(GAME_ID);

        List<GameListItemResponse> result = gameService.getOrganizerGames("org@test.com");

        assertEquals(1, result.size());
    }

    // ─── getPublicGames ───────────────────────────────────────────────────────

    @Test // №47 — Без фильтра по городу: исключает DRAFT и CANCELED, возвращает остальные
    void getPublicGames_noCity_excludesDraftAndCanceled() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game draftGame = makeDraftGame(GAME_ID, organizer);
        Game regOpenGame = makeRegOpenGame(2L, organizer);

        when(gameRepository.findAll()).thenReturn(List.of(draftGame, regOpenGame));
        stubNoSessions(GAME_ID);
        stubNoSessions(2L);

        List<GameListItemResponse> result = gameService.getPublicGames(null);

        assertEquals(1, result.size());
        assertEquals(GameStatus.REGISTRATION_OPEN, result.get(0).status());
    }

    @Test // №48 — С фильтром по городу: вызывается findAllByCityIgnoreCase
    void getPublicGames_withCity_filtersAndReturns() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeRegOpenGame(GAME_ID, organizer);

        when(gameRepository.findAllByCityIgnoreCaseOrderByCreatedAtDesc("Moscow"))
                .thenReturn(List.of(game));
        stubNoSessions(GAME_ID);

        List<GameListItemResponse> result = gameService.getPublicGames("Moscow");

        assertEquals(1, result.size());
    }

    // ─── getOrganizerGameById ─────────────────────────────────────────────────

    @Test // №49 — Успешное получение игры по ID
    void getOrganizerGameById_success_returnsGameResponse() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);

        GameResponse resp = gameService.getOrganizerGameById("org@test.com", GAME_ID);

        assertEquals(GAME_ID, resp.id());
    }

    // ─── getOrganizerGameTasks ────────────────────────────────────────────────

    @Test // №50 — Возвращает список заданий игры
    void getOrganizerGameTasks_success_returnsTasks() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        GameTask task = makeTask(10L, game, 1);
        task.setHints(new java.util.HashSet<>());

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);
        when(gameTaskRepository.findAllByGameIdOrderByOrderIndexAsc(GAME_ID))
                .thenReturn(List.of(task));

        List<GameTaskResponse> result = gameService.getOrganizerGameTasks("org@test.com", GAME_ID);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
    }

    // ─── getOrganizerGameRoutes ───────────────────────────────────────────────

    @Test // №51 — Возвращает список маршрутов игры
    void getOrganizerGameRoutes_success_returnsRoutes() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        TeamGameRoute route = makeRoute(30L, game, 1);
        route.setItems(new java.util.HashSet<>());

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);
        when(teamGameRouteRepository.findAllByGameIdOrderBySlotNumberAsc(GAME_ID))
                .thenReturn(List.of(route));

        List<TeamGameRouteResponse> result = gameService.getOrganizerGameRoutes("org@test.com", GAME_ID);

        assertEquals(1, result.size());
    }

    // ─── getIncomingRegistrations ─────────────────────────────────────────────

    @Test // №52 — Возвращает список PENDING заявок
    void getIncomingRegistrations_success_returnsPendingList() {
        User organizer = makeOrganizer(1L, "org@test.com");
        User captain = makeParticipant(2L, "cap@test.com");
        Team team = makeTeam(10L, captain);
        Game game = makeRegOpenGame(GAME_ID, organizer);
        GameRegistration reg = makeRegistration(5L, game, team, GameRegistrationStatus.PENDING);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);
        when(gameRegistrationRepository.findAllByGameIdAndStatusOrderByCreatedAtDesc(
                GAME_ID, GameRegistrationStatus.PENDING)).thenReturn(List.of(reg));

        List<IncomingGameRegistrationResponse> result =
                gameService.getIncomingRegistrations("org@test.com", GAME_ID);

        assertEquals(1, result.size());
    }

    // ─── getOrganizerGameResults ──────────────────────────────────────────────

    @Test // №53 — Игра FINISHED → возвращает standings
    void getOrganizerGameResults_gameFinished_returnsStandings() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        game.setStatus(GameStatus.FINISHED); // FINISHED → synchronize returns immediately

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTeamSessionRepository.findAllByGameId(GAME_ID)).thenReturn(Collections.emptyList());

        List<GameTeamStandingResponse> result =
                gameService.getOrganizerGameResults("org@test.com", GAME_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test // №54 — Игра ещё не завершена → BadRequestException
    void getOrganizerGameResults_gameNotFinished_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeRegOpenGame(GAME_ID, organizer);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);

        assertThrows(BadRequestException.class,
                () -> gameService.getOrganizerGameResults("org@test.com", GAME_ID));
    }

    // ─── getOrganizerGameStandings ────────────────────────────────────────────

    @Test // №55 — Игра IN_PROGRESS → возвращает live-рейтинг
    void getOrganizerGameStandings_gameInProgress_returnsStandings() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeDraftGame(GAME_ID, organizer);
        game.setStatus(GameStatus.IN_PROGRESS);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        when(gameTeamSessionRepository.existsByGameIdAndStatus(GAME_ID, GameTeamSessionStatus.IN_PROGRESS))
                .thenReturn(true);
        when(gameTeamSessionRepository.findAllByGameId(GAME_ID)).thenReturn(Collections.emptyList());

        List<GameTeamStandingResponse> result =
                gameService.getOrganizerGameStandings("org@test.com", GAME_ID);

        assertNotNull(result);
    }

    @Test // №56 — Игра не IN_PROGRESS → BadRequestException
    void getOrganizerGameStandings_gameNotInProgress_throwsBadRequest() {
        User organizer = makeOrganizer(1L, "org@test.com");
        Game game = makeRegOpenGame(GAME_ID, organizer);

        when(userRepository.findByEmail("org@test.com")).thenReturn(Optional.of(organizer));
        when(gameRepository.findByIdAndOrganizerId(GAME_ID, 1L)).thenReturn(Optional.of(game));
        stubNoSessions(GAME_ID);

        assertThrows(BadRequestException.class,
                () -> gameService.getOrganizerGameStandings("org@test.com", GAME_ID));
    }
}