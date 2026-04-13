package com.example.server.game.service;

import com.example.server.auth.entity.Role;
import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import com.example.server.common.exception.BadRequestException;
import com.example.server.common.exception.ConflictException;
import com.example.server.common.exception.NotFoundException;
import com.example.server.game.dto.CreateGameRequest;
import com.example.server.game.dto.CreateGameTaskHintRequest;
import com.example.server.game.dto.CreateGameTaskRequest;
import com.example.server.game.dto.CreateTeamGameRouteRequest;
import com.example.server.game.dto.CurrentGameTaskHintResponse;
import com.example.server.game.dto.CurrentGameTaskResponse;
import com.example.server.game.dto.GameRegistrationResponse;
import com.example.server.game.dto.GameStartResponse;
import com.example.server.game.dto.GameTeamProgressResponse;
import com.example.server.game.dto.GameTeamStandingResponse;
import com.example.server.game.dto.IncomingGameRegistrationResponse;
import com.example.server.game.dto.GameListItemResponse;
import com.example.server.game.dto.GameResponse;
import com.example.server.game.dto.GameTaskHintResponse;
import com.example.server.game.dto.GameTaskResponse;
import com.example.server.game.dto.AddTaskToRouteRequest;
import com.example.server.game.dto.SubmitTaskKeyRequest;
import com.example.server.game.dto.SubmitTaskKeyResponse;
import com.example.server.game.dto.TeamGameRouteItemResponse;
import com.example.server.game.dto.TeamGameRouteResponse;
import com.example.server.game.dto.TeamGameRegistrationResponse;
import com.example.server.game.dto.UpdateGameRequest;
import com.example.server.game.dto.UpdateGameTaskRequest;
import com.example.server.game.dto.UpdateGameTaskHintRequest;
import com.example.server.game.dto.UpdateTeamGameRouteRequest;
import com.example.server.game.entity.Game;
import com.example.server.game.entity.GameRegistration;
import com.example.server.game.entity.GameRegistrationStatus;
import com.example.server.game.entity.GameStatus;
import com.example.server.game.entity.GameTask;
import com.example.server.game.entity.GameTaskHint;
import com.example.server.game.entity.GameTeamSession;
import com.example.server.game.entity.GameTeamSessionStatus;
import com.example.server.game.entity.TeamGameRoute;
import com.example.server.game.entity.TeamGameRouteItem;
import com.example.server.game.repository.GameRegistrationRepository;
import com.example.server.game.repository.GameRepository;
import com.example.server.game.repository.GameTaskRepository;
import com.example.server.game.repository.GameTaskHintRepository;
import com.example.server.game.repository.GameTeamSessionRepository;
import com.example.server.game.repository.TeamGameRouteItemRepository;
import com.example.server.game.repository.TeamGameRouteRepository;
import com.example.server.team.entity.Team;
import com.example.server.team.entity.TeamMembership;
import com.example.server.team.entity.TeamMembershipRole;
import com.example.server.team.entity.TeamMembershipStatus;
import com.example.server.team.repository.TeamMembershipRepository;
import com.example.server.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
/**
 * Сервис бизнес-логики игр, заявок на участие, маршрутов, заданий и игрового прогресса.
 */
public class GameService {

    private final GameRepository gameRepository;
    private final GameRegistrationRepository gameRegistrationRepository;
    private final GameTeamSessionRepository gameTeamSessionRepository;
    private final GameTaskRepository gameTaskRepository;
    private final GameTaskHintRepository gameTaskHintRepository;
    private final TeamGameRouteRepository teamGameRouteRepository;
    private final TeamGameRouteItemRepository teamGameRouteItemRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Transactional
    /**
     * Создает новую игру для организатора.
     *
     * @param organizerEmail email организатора
     * @param request данные игры
     * @return созданная игра
     */
    public GameResponse createGame(String organizerEmail, CreateGameRequest request) {
        User organizer = getOrganizerByEmail(organizerEmail);
        validateCreateGameRequest(request);

        Game game = new Game();
        game.setTitle(request.title().trim());
        game.setDescription(request.description().trim());
        game.setCity(request.city().trim());
        game.setStatus(GameStatus.DRAFT);
        game.setMinTeamSize(request.minTeamSize());
        game.setMaxTeamSize(request.maxTeamSize());
        game.setTaskFailurePenaltyMinutes(request.taskFailurePenaltyMinutes());
        game.setRouteSlotsCount(request.routeSlotsCount());
        game.setRegistrationStartsAt(request.registrationStartsAt());
        game.setRegistrationEndsAt(request.registrationEndsAt());
        game.setStartsAt(request.startsAt());
        game.setOrganizer(organizer);

        Game savedGame = gameRepository.save(game);
        return buildGameResponse(savedGame);
    }

    @Transactional
    /**
     * Обновляет параметры игры до ее старта.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @param request новые параметры игры
     * @return обновленная игра
     */
    public GameResponse updateGame(String organizerEmail, Long gameId, UpdateGameRequest request) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        validateGameEditable(game);
        validateGameRequest(
                request.minTeamSize(),
                request.maxTeamSize(),
                request.routeSlotsCount(),
                request.registrationStartsAt(),
                request.registrationEndsAt(),
                request.startsAt()
        );
        validateRouteSlotsCountForUpdate(game, request.routeSlotsCount());

        game.setTitle(request.title().trim());
        game.setDescription(request.description().trim());
        game.setCity(request.city().trim());
        game.setMinTeamSize(request.minTeamSize());
        game.setMaxTeamSize(request.maxTeamSize());
        game.setTaskFailurePenaltyMinutes(request.taskFailurePenaltyMinutes());
        game.setRouteSlotsCount(request.routeSlotsCount());
        game.setRegistrationStartsAt(request.registrationStartsAt());
        game.setRegistrationEndsAt(request.registrationEndsAt());
        game.setStartsAt(request.startsAt());

        Game savedGame = gameRepository.save(game);
        return buildGameResponse(savedGame);
    }

    @Transactional
    /**
     * Отменяет игру до её завершения.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @return обновленная игра
     */
    public GameResponse cancelGame(String organizerEmail, Long gameId) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        synchronizeGameLifecycle(game, Instant.now());

        if (game.getStatus() == GameStatus.FINISHED || game.getStatus() == GameStatus.CANCELED) {
            throw new BadRequestException("Отменить можно только незавершенную игру");
        }

        game.setStatus(GameStatus.CANCELED);
        game.setFinishedAt(null);

        Game savedGame = gameRepository.save(game);
        return buildGameResponse(savedGame);
    }

    @Transactional
    /**
     * Создает заявку команды капитана на участие в игре.
     *
     * @param captainEmail email капитана команды
     * @param gameId идентификатор игры
     * @return созданная заявка
     */
    public GameRegistrationResponse submitGameRegistration(String captainEmail, Long gameId) {
        Team team = getCaptainTeam(captainEmail);
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Игра не найдена"));
        synchronizeGameLifecycle(game, Instant.now());

        if (game.getStatus() != GameStatus.REGISTRATION_OPEN) {
            throw new BadRequestException("Подать заявку можно только в игру с открытой регистрацией");
        }

        long activeMembersCount = teamMembershipRepository.countByTeamIdAndStatus(team.getId(), TeamMembershipStatus.ACTIVE);
        if (activeMembersCount < game.getMinTeamSize() || activeMembersCount > game.getMaxTeamSize()) {
            throw new BadRequestException("Размер команды не соответствует требованиям игры");
        }

        GameRegistration registration = gameRegistrationRepository.findByGameIdAndTeamId(gameId, team.getId())
                .orElseGet(GameRegistration::new);

        if (registration.getId() != null) {
            if (registration.getStatus() == GameRegistrationStatus.PENDING
                    || registration.getStatus() == GameRegistrationStatus.APPROVED) {
                throw new ConflictException("Заявка этой команды на игру уже существует");
            }
        }

        registration.setGame(game);
        registration.setTeam(team);
        registration.setStatus(GameRegistrationStatus.PENDING);

        GameRegistration savedRegistration = gameRegistrationRepository.save(registration);
        return buildGameRegistrationResponse(savedRegistration);
    }

    @Transactional
    /**
     * Отменяет pending-заявку команды на участие в игре.
     *
     * @param captainEmail email капитана команды
     * @param gameId идентификатор игры
     * @return обновленная заявка
     */
    public GameRegistrationResponse cancelGameRegistration(String captainEmail, Long gameId) {
        Team team = getCaptainTeam(captainEmail);
        GameRegistration registration = gameRegistrationRepository.findByGameIdAndTeamId(gameId, team.getId())
                .orElseThrow(() -> new NotFoundException("Заявка команды на игру не найдена"));

        if (registration.getStatus() != GameRegistrationStatus.PENDING) {
            throw new BadRequestException("Отменить можно только заявку в статусе PENDING");
        }

        registration.setStatus(GameRegistrationStatus.CANCELED);
        GameRegistration savedRegistration = gameRegistrationRepository.save(registration);
        return buildGameRegistrationResponse(savedRegistration);
    }

    @Transactional
    /**
     * Подтверждает заявку команды на участие в игре.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @param registrationId идентификатор заявки
     * @return обновленная заявка
     */
    public GameRegistrationResponse approveRegistration(String organizerEmail, Long gameId, Long registrationId) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        synchronizeGameLifecycle(game, Instant.now());

        if (game.getStatus() != GameStatus.REGISTRATION_OPEN
                && game.getStatus() != GameStatus.REGISTRATION_CLOSED) {
            throw new BadRequestException("Подтверждать заявки можно только до начала игры");
        }

        GameRegistration registration = getPendingRegistration(gameId, registrationId);
        List<TeamGameRoute> freeRoutes = teamGameRouteRepository.findAllByGameIdAndAssignedTeamIsNullOrderBySlotNumberAsc(gameId);

        if (freeRoutes.isEmpty()) {
            throw new BadRequestException("Нет свободных маршрутов. Сначала подготовьте маршруты для игры");
        }

        TeamGameRoute assignedRoute = freeRoutes.get(ThreadLocalRandom.current().nextInt(freeRoutes.size()));

        registration.setStatus(GameRegistrationStatus.APPROVED);
        assignedRoute.setAssignedTeam(registration.getTeam());
        teamGameRouteRepository.save(assignedRoute);
        GameRegistration savedRegistration = gameRegistrationRepository.save(registration);
        return buildGameRegistrationResponse(savedRegistration);
    }

    @Transactional
    /**
     * Отклоняет заявку команды на участие в игре.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @param registrationId идентификатор заявки
     * @return обновленная заявка
     */
    public GameRegistrationResponse rejectRegistration(String organizerEmail, Long gameId, Long registrationId) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        synchronizeGameLifecycle(game, Instant.now());

        if (game.getStatus() != GameStatus.REGISTRATION_OPEN
                && game.getStatus() != GameStatus.REGISTRATION_CLOSED) {
            throw new BadRequestException("Отклонять заявки можно только до начала игры");
        }

        GameRegistration registration = getPendingRegistration(gameId, registrationId);

        registration.setStatus(GameRegistrationStatus.REJECTED);
        GameRegistration savedRegistration = gameRegistrationRepository.save(registration);
        return buildGameRegistrationResponse(savedRegistration);
    }

    @Transactional
    /**
     * Запускает игру и создает игровые сессии для всех подтвержденных команд.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @return результат запуска игры
     */
    public GameStartResponse startGame(String organizerEmail, Long gameId) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        synchronizeGameLifecycle(game, Instant.now());

        if (game.getStatus() != GameStatus.IN_PROGRESS && game.getStatus() != GameStatus.FINISHED) {
            throw new BadRequestException("Игра ещё не может быть автоматически запущена");
        }

        int startedSessionsCount = gameTeamSessionRepository.findAllByGameId(gameId).size();
        Instant startedAt = gameTeamSessionRepository.findAllByGameId(gameId).stream()
                .map(GameTeamSession::getStartedAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(game.getStartsAt());

        return new GameStartResponse(
                game.getId(),
                game.getTitle(),
                startedSessionsCount,
                startedAt
        );
    }

    @Transactional
    /**
     * Создает новое задание в игре.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @param request данные задания
     * @return созданное задание
     */
    public GameTaskResponse createTask(String organizerEmail, Long gameId, CreateGameTaskRequest request) {
        Game game = getOrganizerGame(organizerEmail, gameId);

        boolean orderTaken = gameTaskRepository.findAllByGameIdOrderByOrderIndexAsc(gameId).stream()
                .anyMatch(task -> task.getOrderIndex().equals(request.orderIndex()));
        if (orderTaken) {
            throw new ConflictException("Задание с таким порядком уже существует");
        }

        GameTask task = new GameTask();
        task.setGame(game);
        task.setTitle(request.title().trim());
        task.setRiddleText(request.riddleText().trim());
        task.setAnswerKey(request.answerKey().trim());
        task.setOrderIndex(request.orderIndex());
        task.setTimeLimitMinutes(request.timeLimitMinutes());
        task.setFailurePenaltyMinutes(request.failurePenaltyMinutes());

        GameTask savedTask = gameTaskRepository.save(task);
        return buildGameTaskResponse(savedTask);
    }

    @Transactional
    public GameTaskResponse updateTask(String organizerEmail, Long gameId, Long taskId, UpdateGameTaskRequest request) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        validateGameEditable(game);

        GameTask task = gameTaskRepository.findByIdAndGameId(taskId, gameId)
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        boolean orderTaken = gameTaskRepository.findAllByGameIdOrderByOrderIndexAsc(gameId).stream()
                .anyMatch(existingTask ->
                        !existingTask.getId().equals(taskId) && existingTask.getOrderIndex().equals(request.orderIndex()));
        if (orderTaken) {
            throw new ConflictException("Задание с таким порядком уже существует");
        }

        task.setTitle(request.title().trim());
        task.setRiddleText(request.riddleText().trim());
        task.setAnswerKey(request.answerKey().trim());
        task.setOrderIndex(request.orderIndex());
        task.setTimeLimitMinutes(request.timeLimitMinutes());
        task.setFailurePenaltyMinutes(request.failurePenaltyMinutes());

        GameTask savedTask = gameTaskRepository.save(task);
        return buildGameTaskResponse(savedTask);
    }

    @Transactional
    public void deleteTask(String organizerEmail, Long gameId, Long taskId) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        validateGameEditable(game);

        GameTask task = gameTaskRepository.findByIdAndGameId(taskId, gameId)
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        if (teamGameRouteItemRepository.existsByTaskIdAndRouteGameId(taskId, gameId)) {
            throw new ConflictException("Нельзя удалить задание, которое уже используется в маршруте");
        }

        gameTaskRepository.delete(task);
    }

    @Transactional
    /**
     * Добавляет подсказку к заданию игры.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @param taskId идентификатор задания
     * @param request данные подсказки
     * @return созданная подсказка
     */
    public GameTaskHintResponse addTaskHint(String organizerEmail, Long gameId, Long taskId, CreateGameTaskHintRequest request) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        validateGameEditable(game);
        GameTask task = gameTaskRepository.findByIdAndGameId(taskId, gameId)
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        List<GameTaskHint> existingHints = gameTaskHintRepository.findAllByTaskIdOrderByOrderIndexAsc(taskId);
        if (existingHints.size() >= 2) {
            throw new BadRequestException("Для задания можно создать только 2 подсказки");
        }

        boolean orderTaken = existingHints.stream().anyMatch(hint -> hint.getOrderIndex().equals(request.orderIndex()));
        if (orderTaken) {
            throw new ConflictException("Подсказка с таким порядком уже существует");
        }

        GameTaskHint hint = new GameTaskHint();
        hint.setTask(task);
        hint.setText(request.text().trim());
        hint.setOrderIndex(request.orderIndex());
        hint.setDelayMinutesFromPreviousHint(request.delayMinutesFromPreviousHint());

        GameTaskHint savedHint = gameTaskHintRepository.save(hint);
        return buildGameTaskHintResponse(savedHint);
    }

    @Transactional
    public GameTaskHintResponse updateTaskHint(String organizerEmail, Long gameId, Long taskId, Long hintId, UpdateGameTaskHintRequest request) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        validateGameEditable(game);
        gameTaskRepository.findByIdAndGameId(taskId, gameId)
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        GameTaskHint hint = gameTaskHintRepository.findByIdAndTaskId(hintId, taskId)
                .orElseThrow(() -> new NotFoundException("Подсказка не найдена"));

        List<GameTaskHint> existingHints = gameTaskHintRepository.findAllByTaskIdOrderByOrderIndexAsc(taskId);
        boolean orderTaken = existingHints.stream()
                .anyMatch(existingHint ->
                        !existingHint.getId().equals(hintId) && existingHint.getOrderIndex().equals(request.orderIndex()));
        if (orderTaken) {
            throw new ConflictException("Подсказка с таким порядком уже существует");
        }

        hint.setText(request.text().trim());
        hint.setOrderIndex(request.orderIndex());
        hint.setDelayMinutesFromPreviousHint(request.delayMinutesFromPreviousHint());

        GameTaskHint savedHint = gameTaskHintRepository.save(hint);
        return buildGameTaskHintResponse(savedHint);
    }

    @Transactional
    public void deleteTaskHint(String organizerEmail, Long gameId, Long taskId, Long hintId) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        validateGameEditable(game);
        gameTaskRepository.findByIdAndGameId(taskId, gameId)
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        GameTaskHint hint = gameTaskHintRepository.findByIdAndTaskId(hintId, taskId)
                .orElseThrow(() -> new NotFoundException("Подсказка не найдена"));

        gameTaskHintRepository.delete(hint);
    }

    @Transactional
    /**
     * Создает маршрут заданий для конкретной команды в рамках игры.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @param request данные маршрута
     * @return созданный маршрут
     */
    public TeamGameRouteResponse createRoute(String organizerEmail, Long gameId, CreateTeamGameRouteRequest request) {
        Game game = getOrganizerGame(organizerEmail, gameId);

        int slotNumber = Math.toIntExact(request.slotNumber());
        if (slotNumber < 1 || slotNumber > game.getRouteSlotsCount()) {
            throw new BadRequestException("Номер маршрута должен быть в пределах количества маршрутов игры");
        }

        if (teamGameRouteRepository.findByGameIdAndSlotNumber(gameId, slotNumber).isPresent()) {
            throw new ConflictException("Маршрут для этого слота уже существует");
        }

        TeamGameRoute route = new TeamGameRoute();
        route.setGame(game);
        route.setSlotNumber(slotNumber);
        route.setName(request.name().trim());

        TeamGameRoute savedRoute = teamGameRouteRepository.save(route);
        return buildTeamGameRouteResponse(savedRoute);
    }

    @Transactional
    public TeamGameRouteResponse updateRoute(String organizerEmail, Long gameId, Long routeId, UpdateTeamGameRouteRequest request) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        validateGameEditable(game);

        TeamGameRoute route = teamGameRouteRepository.findByIdAndGameId(routeId, gameId)
                .orElseThrow(() -> new NotFoundException("Маршрут не найден"));
        route.setName(request.name().trim());

        TeamGameRoute savedRoute = teamGameRouteRepository.save(route);
        return buildTeamGameRouteResponse(savedRoute);
    }

    @Transactional
    public void deleteRoute(String organizerEmail, Long gameId, Long routeId) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        validateGameEditable(game);

        TeamGameRoute route = teamGameRouteRepository.findByIdAndGameId(routeId, gameId)
                .orElseThrow(() -> new NotFoundException("Маршрут не найден"));

        if (route.getAssignedTeam() != null) {
            throw new ConflictException("Нельзя удалить маршрут, который уже назначен команде");
        }

        teamGameRouteRepository.delete(route);
    }

    @Transactional
    /**
     * Добавляет задание в маршрут команды.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @param routeId идентификатор маршрута
     * @param request данные элемента маршрута
     * @return обновленный маршрут
     */
    public TeamGameRouteResponse addTaskToRoute(String organizerEmail, Long gameId, Long routeId, AddTaskToRouteRequest request) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        validateGameEditable(game);
        TeamGameRoute route = teamGameRouteRepository.findByIdAndGameId(routeId, gameId)
                .orElseThrow(() -> new NotFoundException("Маршрут не найден"));
        GameTask task = gameTaskRepository.findByIdAndGameId(request.taskId(), gameId)
                .orElseThrow(() -> new NotFoundException("Задание не найдено"));

        List<TeamGameRouteItem> routeItems = teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(routeId);
        boolean orderTaken = routeItems.stream().anyMatch(item -> item.getOrderIndex().equals(request.orderIndex()));
        if (orderTaken) {
            throw new ConflictException("Элемент маршрута с таким порядком уже существует");
        }

        boolean taskTaken = routeItems.stream().anyMatch(item -> item.getTask().getId().equals(task.getId()));
        if (taskTaken) {
            throw new ConflictException("Это задание уже добавлено в маршрут");
        }

        if (teamGameRouteItemRepository.existsByTaskIdAndRouteGameId(task.getId(), gameId)) {
            throw new ConflictException("Это задание уже используется в другом маршруте игры");
        }

        TeamGameRouteItem routeItem = new TeamGameRouteItem();
        routeItem.setRoute(route);
        routeItem.setTask(task);
        routeItem.setOrderIndex(request.orderIndex());
        teamGameRouteItemRepository.save(routeItem);

        return buildTeamGameRouteResponse(route);
    }

    @Transactional
    public TeamGameRouteResponse removeTaskFromRoute(String organizerEmail, Long gameId, Long routeId, Long itemId) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        validateGameEditable(game);

        TeamGameRoute route = teamGameRouteRepository.findByIdAndGameId(routeId, gameId)
                .orElseThrow(() -> new NotFoundException("Маршрут не найден"));
        TeamGameRouteItem routeItem = teamGameRouteItemRepository.findByIdAndRouteId(itemId, routeId)
                .orElseThrow(() -> new NotFoundException("Элемент маршрута не найден"));

        teamGameRouteItemRepository.delete(routeItem);
        return buildTeamGameRouteResponse(route);
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает список игр текущего организатора.
     *
     * @param organizerEmail email организатора
     * @return список игр
     */
    public List<GameListItemResponse> getOrganizerGames(String organizerEmail) {
        User organizer = getOrganizerByEmail(organizerEmail);

        return gameRepository.findAllByOrganizerIdOrderByCreatedAtDesc(organizer.getId()).stream()
                .map(game -> synchronizeGameLifecycle(game, Instant.now()))
                .map(this::buildGameListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает публичный список игр с опциональной фильтрацией по городу.
     *
     * @param city город для фильтрации
     * @return список доступных игр
     */
    public List<GameListItemResponse> getPublicGames(String city) {
        List<Game> games = city == null || city.isBlank()
                ? gameRepository.findAll()
                : gameRepository.findAllByCityIgnoreCaseOrderByCreatedAtDesc(city.trim());

        return games.stream()
                .map(game -> synchronizeGameLifecycle(game, Instant.now()))
                .filter(game -> game.getStatus() != GameStatus.DRAFT && game.getStatus() != GameStatus.CANCELED)
                .sorted(Comparator.comparing(Game::getCreatedAt).reversed())
                .map(this::buildGameListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает подробную информацию об игре текущего организатора.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @return данные игры
     */
    public GameResponse getOrganizerGameById(String organizerEmail, Long gameId) {
        return buildGameResponse(synchronizeGameLifecycle(getOrganizerGame(organizerEmail, gameId), Instant.now()));
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает список заданий игры текущего организатора.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @return список заданий игры
     */
    public List<GameTaskResponse> getOrganizerGameTasks(String organizerEmail, Long gameId) {
        synchronizeGameLifecycle(getOrganizerGame(organizerEmail, gameId), Instant.now());

        return gameTaskRepository.findAllByGameIdOrderByOrderIndexAsc(gameId).stream()
                .map(this::buildGameTaskResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает список маршрутов команд для игры текущего организатора.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @return список маршрутов игры
     */
    public List<TeamGameRouteResponse> getOrganizerGameRoutes(String organizerEmail, Long gameId) {
        synchronizeGameLifecycle(getOrganizerGame(organizerEmail, gameId), Instant.now());

        return teamGameRouteRepository.findAllByGameIdOrderBySlotNumberAsc(gameId).stream()
                .map(this::buildTeamGameRouteResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает входящие заявки команд для указанной игры организатора.
     *
     * @param organizerEmail email организатора
     * @param gameId идентификатор игры
     * @return список заявок команд
     */
    public List<IncomingGameRegistrationResponse> getIncomingRegistrations(String organizerEmail, Long gameId) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        synchronizeGameLifecycle(game, Instant.now());

        return gameRegistrationRepository.findAllByGameIdAndStatusOrderByCreatedAtDesc(
                        game.getId(),
                        GameRegistrationStatus.PENDING
                ).stream()
                .map(this::buildIncomingGameRegistrationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает все заявки текущей команды на игры.
     *
     * @param captainEmail email капитана команды
     * @return список заявок команды
     */
    public List<TeamGameRegistrationResponse> getTeamRegistrations(String captainEmail) {
        Team team = getCaptainTeam(captainEmail);

        return gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc(team.getId()).stream()
                .peek(registration -> synchronizeGameLifecycle(registration.getGame(), Instant.now()))
                .map(this::buildTeamGameRegistrationResponse)
                .toList();
    }

    @Transactional
    /**
     * Возвращает текущее активное задание команды пользователя.
     * Перед формированием ответа синхронизирует таймаут задания.
     *
     * @param userEmail email участника команды
     * @return данные текущего задания
     */
    public CurrentGameTaskResponse getCurrentTask(String userEmail) {
        Team team = getActiveTeam(userEmail);
        synchronizeTeamGamesLifecycle(team, Instant.now());
        GameTeamSession session = gameTeamSessionRepository.findByTeamIdAndStatus(team.getId(), GameTeamSessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new NotFoundException("У команды нет активной игровой сессии"));

        synchronizeSessionWithTimeout(session, Instant.now());

        if (session.getStatus() != GameTeamSessionStatus.IN_PROGRESS) {
            throw new NotFoundException("У команды нет активного задания");
        }

        Instant now = Instant.now();
        Instant taskStartedAt = session.getCurrentTaskStartedAt();
        Instant deadlineAt = taskStartedAt.plusSeconds((long) session.getCurrentTask().getTimeLimitMinutes() * 60);
        long remainingSeconds = Math.max(0, deadlineAt.getEpochSecond() - now.getEpochSecond());

        List<TeamGameRouteItem> routeItems = teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(session.getRoute().getId());
        List<CurrentGameTaskHintResponse> availableHints = buildAvailableHints(session.getCurrentTask(), taskStartedAt, now);

        return new CurrentGameTaskResponse(
                session.getId(),
                session.getGame().getId(),
                session.getGame().getTitle(),
                session.getTeam().getId(),
                session.getCurrentTask().getId(),
                session.getCurrentTask().getTitle(),
                session.getCurrentTask().getRiddleText(),
                session.getCurrentOrderIndex(),
                routeItems.size(),
                session.getCurrentTask().getTimeLimitMinutes(),
                session.getCurrentTask().getFailurePenaltyMinutes(),
                session.getTotalPenaltyMinutes(),
                taskStartedAt,
                deadlineAt,
                remainingSeconds,
                session.getStatus(),
                "ACTIVE",
                availableHints
        );
    }

    @Transactional
    /**
     * Проверяет ключ текущего задания и переводит команду на следующий шаг маршрута.
     *
     * @param captainEmail email капитана команды
     * @param request ключ задания
     * @return результат проверки ключа
     */
    public SubmitTaskKeyResponse submitTaskKey(String captainEmail, SubmitTaskKeyRequest request) {
        Team team = getCaptainTeam(captainEmail);
        synchronizeTeamGamesLifecycle(team, Instant.now());
        GameTeamSession session = gameTeamSessionRepository.findByTeamIdAndStatus(team.getId(), GameTeamSessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new NotFoundException("У команды нет активной игровой сессии"));

        synchronizeSessionWithTimeout(session, Instant.now());

        if (session.getStatus() != GameTeamSessionStatus.IN_PROGRESS) {
            throw new BadRequestException("Текущее задание уже завершено по таймауту");
        }

        String providedKey = normalizeAnswerKey(request.key());
        String expectedKey = normalizeAnswerKey(session.getCurrentTask().getAnswerKey());

        if (!expectedKey.equals(providedKey)) {
            throw new BadRequestException("Неверный ключ");
        }

        Instant submittedAt = Instant.now();
        GameTask completedTask = session.getCurrentTask();
        Integer completedOrderIndex = session.getCurrentOrderIndex();
        List<TeamGameRouteItem> routeItems = teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(session.getRoute().getId());

        TeamGameRouteItem nextRouteItem = routeItems.stream()
                .filter(item -> item.getOrderIndex() > completedOrderIndex)
                .findFirst()
                .orElse(null);

        if (nextRouteItem == null) {
            session.setStatus(GameTeamSessionStatus.FINISHED);
            session.setFinishedAt(submittedAt);
            gameTeamSessionRepository.save(session);

            if (!gameTeamSessionRepository.existsByGameIdAndStatus(session.getGame().getId(), GameTeamSessionStatus.IN_PROGRESS)) {
                Game game = session.getGame();
                game.setStatus(GameStatus.FINISHED);
                game.setFinishedAt(submittedAt);
                gameRepository.save(game);
            }

            return new SubmitTaskKeyResponse(
                    session.getId(),
                team.getId(),
                completedTask.getId(),
                completedTask.getTitle(),
                completedOrderIndex,
                session.getTotalPenaltyMinutes(),
                true,
                session.getStatus(),
                null,
                    null,
                    null,
                    submittedAt
            );
        }

        session.setCurrentRouteItem(nextRouteItem);
        session.setCurrentTask(nextRouteItem.getTask());
        session.setCurrentOrderIndex(nextRouteItem.getOrderIndex());
        session.setCurrentTaskStartedAt(submittedAt);
        gameTeamSessionRepository.save(session);

        return new SubmitTaskKeyResponse(
                session.getId(),
                team.getId(),
                completedTask.getId(),
                completedTask.getTitle(),
                completedOrderIndex,
                session.getTotalPenaltyMinutes(),
                false,
                session.getStatus(),
                nextRouteItem.getTask().getId(),
                nextRouteItem.getTask().getTitle(),
                nextRouteItem.getOrderIndex(),
                submittedAt
        );
    }

    @Transactional
    /**
     * Рассчитывает текущий прогресс команды и общий зачет по игре.
     *
     * @param userEmail email участника команды
     * @return прогресс команды и standings игры
     */
    public GameTeamProgressResponse getMyTeamProgress(String userEmail) {
        Team team = getActiveTeam(userEmail);
        synchronizeTeamGamesLifecycle(team, Instant.now());
        GameTeamSession teamSession = gameTeamSessionRepository.findTopByTeamIdOrderByStartedAtDesc(team.getId())
                .orElseThrow(() -> new NotFoundException("У команды нет игровой сессии"));

        List<GameTeamSession> sessions = gameTeamSessionRepository.findAllByGameId(teamSession.getGame().getId());
        Instant now = Instant.now();

        for (GameTeamSession session : sessions) {
            synchronizeSessionWithTimeout(session, now);
        }

        sessions.sort(Comparator
                .comparingLong(this::getTotalScoreSeconds)
                .thenComparing(GameTeamSession::getStartedAt));

        List<GameTeamStandingResponse> standings = new ArrayList<>();
        int currentPlace = 0;

        for (int i = 0; i < sessions.size(); i++) {
            GameTeamSession session = sessions.get(i);
            int place = i + 1;
            standings.add(buildGameTeamStandingResponse(session, place));

            if (session.getId().equals(teamSession.getId())) {
                currentPlace = place;
                teamSession = session;
            }
        }

        return new GameTeamProgressResponse(
                teamSession.getGame().getId(),
                teamSession.getGame().getTitle(),
                teamSession.getTeam().getId(),
                teamSession.getTeam().getName(),
                currentPlace,
                getCompletedTasksCount(teamSession),
                getTotalTasksCount(teamSession),
                teamSession.getTotalPenaltyMinutes(),
                getElapsedSeconds(teamSession),
                getTotalScoreSeconds(teamSession),
                teamSession.getStatus(),
                teamSession.getStartedAt(),
                teamSession.getFinishedAt(),
                standings
        );
    }

    private void validateCreateGameRequest(CreateGameRequest request) {
        validateGameRequest(
                request.minTeamSize(),
                request.maxTeamSize(),
                request.routeSlotsCount(),
                request.registrationStartsAt(),
                request.registrationEndsAt(),
                request.startsAt()
        );
    }

    private User getOrganizerByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (user.getRole() != Role.ORGANIZER) {
            throw new BadRequestException("Управлять играми могут только организаторы");
        }

        return user;
    }

    private Game getOrganizerGame(String organizerEmail, Long gameId) {
        User organizer = getOrganizerByEmail(organizerEmail);

        return gameRepository.findByIdAndOrganizerId(gameId, organizer.getId())
                .orElseThrow(() -> new NotFoundException("Игра не найдена"));
    }

    private Team getCaptainTeam(String captainEmail) {
        Team team = getActiveTeam(captainEmail);
        User captain = userRepository.findByEmail(captainEmail)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(team.getId(), captain.getId())
                .orElseThrow(() -> new NotFoundException("У пользователя нет команды"));

        if (membership.getRole() != TeamMembershipRole.CAPTAIN) {
            throw new BadRequestException("Подавать заявку на игру может только капитан команды");
        }

        return team;
    }

    private Team getActiveTeam(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        TeamMembership membership = teamMembershipRepository.findByUserIdAndStatus(user.getId(), TeamMembershipStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("У пользователя нет команды"));

        return membership.getTeam();
    }

    private GameRegistration getPendingRegistration(Long gameId, Long registrationId) {
        GameRegistration registration = gameRegistrationRepository.findByIdAndGameId(registrationId, gameId)
                .orElseThrow(() -> new NotFoundException("Заявка на игру не найдена"));

        if (registration.getStatus() != GameRegistrationStatus.PENDING) {
            throw new BadRequestException("Можно обработать только заявку в статусе PENDING");
        }

        return registration;
    }

    private List<CurrentGameTaskHintResponse> buildAvailableHints(GameTask task, Instant taskStartedAt, Instant now) {
        List<GameTaskHint> hints = gameTaskHintRepository.findAllByTaskIdOrderByOrderIndexAsc(task.getId());
        long cumulativeDelaySeconds = 0;
        List<CurrentGameTaskHintResponse> availableHints = new ArrayList<>();

        for (GameTaskHint hint : hints) {
            cumulativeDelaySeconds += (long) hint.getDelayMinutesFromPreviousHint() * 60;
            Instant unlockedAt = taskStartedAt.plusSeconds(cumulativeDelaySeconds);

            if (!unlockedAt.isAfter(now)) {
                availableHints.add(new CurrentGameTaskHintResponse(
                        hint.getId(),
                        hint.getOrderIndex(),
                        hint.getText(),
                        unlockedAt
                ));
            }
        }

        return availableHints;
    }

    private void synchronizeSessionWithTimeout(GameTeamSession session, Instant referenceNow) {
        while (session.getStatus() == GameTeamSessionStatus.IN_PROGRESS) {
            Instant deadlineAt = session.getCurrentTaskStartedAt()
                    .plusSeconds((long) session.getCurrentTask().getTimeLimitMinutes() * 60);

            if (deadlineAt.isAfter(referenceNow)) {
                return;
            }

            session.setTotalPenaltyMinutes(session.getTotalPenaltyMinutes() + session.getCurrentTask().getFailurePenaltyMinutes());

            List<TeamGameRouteItem> routeItems = teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(session.getRoute().getId());
            TeamGameRouteItem nextRouteItem = routeItems.stream()
                    .filter(item -> item.getOrderIndex() > session.getCurrentOrderIndex())
                    .findFirst()
                    .orElse(null);

            if (nextRouteItem == null) {
                session.setStatus(GameTeamSessionStatus.FINISHED);
                session.setFinishedAt(deadlineAt);
                gameTeamSessionRepository.save(session);
                finishGameIfNoActiveSessions(session.getGame(), deadlineAt);
                return;
            }

            session.setCurrentRouteItem(nextRouteItem);
            session.setCurrentTask(nextRouteItem.getTask());
            session.setCurrentOrderIndex(nextRouteItem.getOrderIndex());
            session.setCurrentTaskStartedAt(deadlineAt);
            gameTeamSessionRepository.save(session);
        }
    }

    private String normalizeAnswerKey(String answerKey) {
        return answerKey.trim().toUpperCase(Locale.ROOT);
    }

    private void finishGameIfNoActiveSessions(Game game, Instant finishedAt) {
        if (!gameTeamSessionRepository.existsByGameIdAndStatus(game.getId(), GameTeamSessionStatus.IN_PROGRESS)) {
            game.setStatus(GameStatus.FINISHED);
            game.setFinishedAt(finishedAt);
            gameRepository.save(game);
        }
    }

    private void synchronizeTeamGamesLifecycle(Team team, Instant referenceNow) {
        gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc(team.getId()).stream()
                .map(GameRegistration::getGame)
                .distinct()
                .forEach(game -> synchronizeGameLifecycle(game, referenceNow));
    }

    private Game synchronizeGameLifecycle(Game game, Instant referenceNow) {
        if (game.getStatus() == GameStatus.CANCELED || game.getStatus() == GameStatus.FINISHED) {
            return game;
        }

        if (gameTeamSessionRepository.existsByGameIdAndStatus(game.getId(), GameTeamSessionStatus.IN_PROGRESS)) {
            if (game.getStatus() != GameStatus.IN_PROGRESS) {
                game.setStatus(GameStatus.IN_PROGRESS);
                game.setFinishedAt(null);
                gameRepository.save(game);
            }
            return game;
        }

        if (gameTeamSessionRepository.existsByGameId(game.getId())) {
            if (game.getStatus() != GameStatus.FINISHED) {
                game.setStatus(GameStatus.FINISHED);
                if (game.getFinishedAt() == null) {
                    game.setFinishedAt(referenceNow);
                }
                gameRepository.save(game);
            }
            return game;
        }

        GameStatus computedStatus = computePlannedStatus(game, referenceNow);
        if (computedStatus == GameStatus.IN_PROGRESS) {
            if (canAutomaticallyStartGame(game)) {
                return launchGameSessions(game, referenceNow);
            }

            computedStatus = GameStatus.REGISTRATION_CLOSED;
        }

        if (game.getStatus() != computedStatus) {
            game.setStatus(computedStatus);
            if (computedStatus != GameStatus.FINISHED) {
                game.setFinishedAt(null);
            }
            gameRepository.save(game);
        }

        return game;
    }

    private GameStatus computePlannedStatus(Game game, Instant referenceNow) {
        if (game.getStartsAt() != null && !referenceNow.isBefore(game.getStartsAt())) {
            return GameStatus.IN_PROGRESS;
        }

        if (game.getRegistrationEndsAt() != null && !referenceNow.isBefore(game.getRegistrationEndsAt())) {
            return GameStatus.REGISTRATION_CLOSED;
        }

        if (game.getRegistrationStartsAt() == null || !referenceNow.isBefore(game.getRegistrationStartsAt())) {
            return GameStatus.REGISTRATION_OPEN;
        }

        return GameStatus.DRAFT;
    }

    private boolean canAutomaticallyStartGame(Game game) {
        if (!hasConsistentRoutes(game)) {
            return false;
        }

        List<GameRegistration> approvedRegistrations = gameRegistrationRepository
                .findAllByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameRegistrationStatus.APPROVED);

        if (approvedRegistrations.isEmpty()) {
            return false;
        }

        for (GameRegistration registration : approvedRegistrations) {
            TeamGameRoute route = teamGameRouteRepository.findByGameIdAndAssignedTeamId(game.getId(), registration.getTeam().getId())
                    .orElse(null);
            if (route == null) {
                return false;
            }

            List<TeamGameRouteItem> routeItems = teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(route.getId());
            if (routeItems.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private boolean hasConsistentRoutes(Game game) {
        List<TeamGameRoute> routes = teamGameRouteRepository.findAllByGameIdOrderBySlotNumberAsc(game.getId());
        if (routes.size() != game.getRouteSlotsCount()) {
            return false;
        }

        Integer expectedTasksCount = null;
        for (TeamGameRoute route : routes) {
          int routeTasksCount = teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(route.getId()).size();
          if (routeTasksCount == 0) {
              return false;
          }

          if (expectedTasksCount == null) {
              expectedTasksCount = routeTasksCount;
              continue;
          }

          if (!expectedTasksCount.equals(routeTasksCount)) {
              return false;
          }
        }

        return true;
    }

    private Game launchGameSessions(Game game, Instant startedAt) {
        if (gameTeamSessionRepository.existsByGameId(game.getId())) {
            game.setStatus(gameTeamSessionRepository.existsByGameIdAndStatus(game.getId(), GameTeamSessionStatus.IN_PROGRESS)
                    ? GameStatus.IN_PROGRESS
                    : GameStatus.FINISHED);
            gameRepository.save(game);
            return game;
        }

        List<GameRegistration> approvedRegistrations = gameRegistrationRepository
                .findAllByGameIdAndStatusOrderByCreatedAtDesc(game.getId(), GameRegistrationStatus.APPROVED);

        for (GameRegistration registration : approvedRegistrations) {
            Team team = registration.getTeam();
            TeamGameRoute route = teamGameRouteRepository.findByGameIdAndAssignedTeamId(game.getId(), team.getId())
                    .orElseThrow(() -> new BadRequestException("Для подтвержденной команды не назначен маршрут заданий"));

            List<TeamGameRouteItem> routeItems = teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(route.getId());
            if (routeItems.isEmpty()) {
                throw new BadRequestException("Маршрут подтвержденной команды не содержит заданий");
            }

            TeamGameRouteItem firstRouteItem = routeItems.getFirst();
            GameTask firstTask = firstRouteItem.getTask();

            GameTeamSession session = new GameTeamSession();
            session.setGame(game);
            session.setTeam(team);
            session.setRoute(route);
            session.setCurrentRouteItem(firstRouteItem);
            session.setCurrentTask(firstTask);
            session.setCurrentOrderIndex(firstRouteItem.getOrderIndex());
            session.setStatus(GameTeamSessionStatus.IN_PROGRESS);
            session.setStartedAt(startedAt);
            session.setCurrentTaskStartedAt(startedAt);
            session.setTotalPenaltyMinutes(0);
            gameTeamSessionRepository.save(session);
        }

        game.setStatus(GameStatus.IN_PROGRESS);
        game.setFinishedAt(null);
        gameRepository.save(game);
        return game;
    }

    private int getCompletedTasksCount(GameTeamSession session) {
        int totalTasksCount = getTotalTasksCount(session);

        return switch (session.getStatus()) {
            case FINISHED -> totalTasksCount;
            case IN_PROGRESS, CANCELED -> Math.max(0, session.getCurrentOrderIndex() - 1);
        };
    }

    private int getTotalTasksCount(GameTeamSession session) {
        return teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(session.getRoute().getId()).size();
    }

    private long getElapsedSeconds(GameTeamSession session) {
        Instant end = session.getFinishedAt() != null ? session.getFinishedAt() : Instant.now();
        return Math.max(0, end.getEpochSecond() - session.getStartedAt().getEpochSecond());
    }

    private long getTotalScoreSeconds(GameTeamSession session) {
        return getElapsedSeconds(session) + (long) session.getTotalPenaltyMinutes() * 60;
    }

    private GameTeamStandingResponse buildGameTeamStandingResponse(GameTeamSession session, int place) {
        return new GameTeamStandingResponse(
                place,
                session.getTeam().getId(),
                session.getTeam().getName(),
                getCompletedTasksCount(session),
                getTotalTasksCount(session),
                session.getTotalPenaltyMinutes(),
                getElapsedSeconds(session),
                getTotalScoreSeconds(session),
                session.getStatus(),
                session.getFinishedAt()
        );
    }

    private void validateGameEditable(Game game) {
        if (game.getStatus() == GameStatus.IN_PROGRESS
                || game.getStatus() == GameStatus.FINISHED
                || game.getStatus() == GameStatus.CANCELED) {
            throw new BadRequestException("Редактировать игру можно только до её старта и завершения");
        }

        if (game.getStartsAt() != null && !game.getStartsAt().isAfter(Instant.now())) {
            throw new BadRequestException("Редактировать игру можно только до её старта");
        }
    }

    private void validateGameRequest(
            Integer minTeamSize,
            Integer maxTeamSize,
            Integer routeSlotsCount,
            Instant registrationStartsAt,
            Instant registrationEndsAt,
            Instant startsAt
    ) {
        if (minTeamSize > maxTeamSize) {
            throw new BadRequestException("Минимальный размер команды не может быть больше максимального");
        }

        if (routeSlotsCount == null || routeSlotsCount < 1) {
            throw new BadRequestException("Количество маршрутов должно быть не меньше 1");
        }

        if (registrationStartsAt != null && registrationEndsAt != null && registrationStartsAt.isAfter(registrationEndsAt)) {
            throw new BadRequestException("Дата начала регистрации не может быть позже даты окончания регистрации");
        }

        if (registrationEndsAt != null && registrationEndsAt.isAfter(startsAt)) {
            throw new BadRequestException("Дата окончания регистрации не может быть позже даты начала игры");
        }
    }

    private void validateRouteSlotsCountForUpdate(Game game, Integer routeSlotsCount) {
        int existingRoutesCount = teamGameRouteRepository.findAllByGameIdOrderBySlotNumberAsc(game.getId()).size();
        if (routeSlotsCount < existingRoutesCount) {
            throw new BadRequestException("Нельзя уменьшить количество маршрутов ниже уже созданных маршрутов");
        }

        long approvedRegistrationsCount = gameRegistrationRepository.findAllByGameIdAndStatusOrderByCreatedAtDesc(
                game.getId(),
                GameRegistrationStatus.APPROVED
        ).size();
        if (routeSlotsCount < approvedRegistrationsCount) {
            throw new BadRequestException("Нельзя уменьшить количество маршрутов ниже числа подтвержденных команд");
        }
    }

    private GameListItemResponse buildGameListItemResponse(Game game) {
        return new GameListItemResponse(
                game.getId(),
                game.getTitle(),
                game.getCity(),
                game.getStatus(),
                game.getMinTeamSize(),
                game.getMaxTeamSize(),
                game.getRouteSlotsCount(),
                game.getRegistrationStartsAt(),
                game.getRegistrationEndsAt(),
                game.getStartsAt(),
                game.getCreatedAt()
        );
    }

    private GameResponse buildGameResponse(Game game) {
        return new GameResponse(
                game.getId(),
                game.getTitle(),
                game.getDescription(),
                game.getCity(),
                game.getStatus(),
                game.getMinTeamSize(),
                game.getMaxTeamSize(),
                game.getTaskFailurePenaltyMinutes(),
                game.getRouteSlotsCount(),
                game.getRegistrationStartsAt(),
                game.getRegistrationEndsAt(),
                game.getStartsAt(),
                game.getFinishedAt(),
                game.getOrganizer().getId(),
                game.getOrganizer().getEmail(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );
    }

    private GameRegistrationResponse buildGameRegistrationResponse(GameRegistration registration) {
        return new GameRegistrationResponse(
                registration.getId(),
                registration.getGame().getId(),
                registration.getGame().getTitle(),
                registration.getTeam().getId(),
                registration.getTeam().getName(),
                registration.getStatus(),
                registration.getCreatedAt(),
                registration.getUpdatedAt()
        );
    }

    private IncomingGameRegistrationResponse buildIncomingGameRegistrationResponse(GameRegistration registration) {
        long activeMembersCount = teamMembershipRepository.countByTeamIdAndStatus(
                registration.getTeam().getId(),
                TeamMembershipStatus.ACTIVE
        );

        return new IncomingGameRegistrationResponse(
                registration.getId(),
                registration.getGame().getId(),
                registration.getTeam().getId(),
                registration.getTeam().getName(),
                registration.getTeam().getCity(),
                registration.getTeam().getCaptain().getId(),
                registration.getTeam().getCaptain().getEmail(),
                Math.toIntExact(activeMembersCount),
                registration.getStatus(),
                registration.getCreatedAt(),
                registration.getUpdatedAt()
        );
    }

    private TeamGameRegistrationResponse buildTeamGameRegistrationResponse(GameRegistration registration) {
        return new TeamGameRegistrationResponse(
                registration.getId(),
                registration.getGame().getId(),
                registration.getGame().getTitle(),
                registration.getGame().getCity(),
                registration.getGame().getStatus(),
                registration.getGame().getMinTeamSize(),
                registration.getGame().getMaxTeamSize(),
                registration.getGame().getStartsAt(),
                registration.getStatus(),
                registration.getCreatedAt(),
                registration.getUpdatedAt()
        );
    }

    private GameTaskResponse buildGameTaskResponse(GameTask task) {
        List<GameTaskHintResponse> hints = gameTaskHintRepository.findAllByTaskIdOrderByOrderIndexAsc(task.getId()).stream()
                .map(this::buildGameTaskHintResponse)
                .toList();

        return new GameTaskResponse(
                task.getId(),
                task.getGame().getId(),
                task.getTitle(),
                task.getRiddleText(),
                task.getAnswerKey(),
                task.getOrderIndex(),
                task.getTimeLimitMinutes(),
                task.getFailurePenaltyMinutes(),
                task.getCreatedAt(),
                hints
        );
    }

    private GameTaskHintResponse buildGameTaskHintResponse(GameTaskHint hint) {
        return new GameTaskHintResponse(
                hint.getId(),
                hint.getOrderIndex(),
                hint.getText(),
                hint.getDelayMinutesFromPreviousHint()
        );
    }

    private TeamGameRouteResponse buildTeamGameRouteResponse(TeamGameRoute route) {
        List<TeamGameRouteItemResponse> items = teamGameRouteItemRepository.findAllByRouteIdOrderByOrderIndexAsc(route.getId()).stream()
                .map(item -> new TeamGameRouteItemResponse(
                        item.getId(),
                        item.getOrderIndex(),
                        item.getTask().getId(),
                        item.getTask().getTitle()
                ))
                .toList();

        return new TeamGameRouteResponse(
                route.getId(),
                route.getGame().getId(),
                route.getSlotNumber(),
                route.getAssignedTeam() != null ? route.getAssignedTeam().getId() : null,
                route.getAssignedTeam() != null ? route.getAssignedTeam().getName() : null,
                route.getName(),
                route.getCreatedAt(),
                items
        );
    }
}
