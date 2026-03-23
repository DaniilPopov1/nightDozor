package com.example.server.game.service;

import com.example.server.auth.entity.Role;
import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import com.example.server.common.exception.BadRequestException;
import com.example.server.common.exception.ConflictException;
import com.example.server.common.exception.NotFoundException;
import com.example.server.game.dto.CreateGameRequest;
import com.example.server.game.dto.GameRegistrationResponse;
import com.example.server.game.dto.IncomingGameRegistrationResponse;
import com.example.server.game.dto.GameListItemResponse;
import com.example.server.game.dto.GameResponse;
import com.example.server.game.dto.TeamGameRegistrationResponse;
import com.example.server.game.dto.UpdateGameRequest;
import com.example.server.game.dto.UpdateGameStatusRequest;
import com.example.server.game.entity.Game;
import com.example.server.game.entity.GameRegistration;
import com.example.server.game.entity.GameRegistrationStatus;
import com.example.server.game.entity.GameStatus;
import com.example.server.game.repository.GameRegistrationRepository;
import com.example.server.game.repository.GameRepository;
import com.example.server.team.entity.Team;
import com.example.server.team.entity.TeamMembership;
import com.example.server.team.entity.TeamMembershipRole;
import com.example.server.team.entity.TeamMembershipStatus;
import com.example.server.team.repository.TeamMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GameService {

    private static final Set<GameStatus> PUBLIC_GAME_STATUSES = EnumSet.of(
            GameStatus.REGISTRATION_OPEN,
            GameStatus.REGISTRATION_CLOSED,
            GameStatus.IN_PROGRESS,
            GameStatus.FINISHED
    );

    private final GameRepository gameRepository;
    private final GameRegistrationRepository gameRegistrationRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;

    @Transactional
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
        game.setRegistrationStartsAt(request.registrationStartsAt());
        game.setRegistrationEndsAt(request.registrationEndsAt());
        game.setStartsAt(request.startsAt());
        game.setOrganizer(organizer);

        Game savedGame = gameRepository.save(game);
        return buildGameResponse(savedGame);
    }

    @Transactional
    public GameResponse updateGame(String organizerEmail, Long gameId, UpdateGameRequest request) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        validateGameEditable(game);
        validateGameRequest(
                request.minTeamSize(),
                request.maxTeamSize(),
                request.registrationStartsAt(),
                request.registrationEndsAt(),
                request.startsAt()
        );

        game.setTitle(request.title().trim());
        game.setDescription(request.description().trim());
        game.setCity(request.city().trim());
        game.setMinTeamSize(request.minTeamSize());
        game.setMaxTeamSize(request.maxTeamSize());
        game.setTaskFailurePenaltyMinutes(request.taskFailurePenaltyMinutes());
        game.setRegistrationStartsAt(request.registrationStartsAt());
        game.setRegistrationEndsAt(request.registrationEndsAt());
        game.setStartsAt(request.startsAt());

        Game savedGame = gameRepository.save(game);
        return buildGameResponse(savedGame);
    }

    @Transactional
    public GameResponse updateGameStatus(String organizerEmail, Long gameId, UpdateGameStatusRequest request) {
        Game game = getOrganizerGame(organizerEmail, gameId);
        applyStatusTransition(game, request.status());

        Game savedGame = gameRepository.save(game);
        return buildGameResponse(savedGame);
    }

    @Transactional
    public GameRegistrationResponse submitGameRegistration(String captainEmail, Long gameId) {
        Team team = getCaptainTeam(captainEmail);
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new NotFoundException("Игра не найдена"));

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
    public GameRegistrationResponse approveRegistration(String organizerEmail, Long gameId, Long registrationId) {
        getOrganizerGame(organizerEmail, gameId);
        GameRegistration registration = getPendingRegistration(gameId, registrationId);

        registration.setStatus(GameRegistrationStatus.APPROVED);
        GameRegistration savedRegistration = gameRegistrationRepository.save(registration);
        return buildGameRegistrationResponse(savedRegistration);
    }

    @Transactional
    public GameRegistrationResponse rejectRegistration(String organizerEmail, Long gameId, Long registrationId) {
        getOrganizerGame(organizerEmail, gameId);
        GameRegistration registration = getPendingRegistration(gameId, registrationId);

        registration.setStatus(GameRegistrationStatus.REJECTED);
        GameRegistration savedRegistration = gameRegistrationRepository.save(registration);
        return buildGameRegistrationResponse(savedRegistration);
    }

    @Transactional(readOnly = true)
    public List<GameListItemResponse> getOrganizerGames(String organizerEmail) {
        User organizer = getOrganizerByEmail(organizerEmail);

        return gameRepository.findAllByOrganizerIdOrderByCreatedAtDesc(organizer.getId()).stream()
                .map(this::buildGameListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GameListItemResponse> getPublicGames(String city) {
        List<Game> games = city == null || city.isBlank()
                ? gameRepository.findAllByStatusInOrderByCreatedAtDesc(PUBLIC_GAME_STATUSES)
                : gameRepository.findAllByCityIgnoreCaseAndStatusInOrderByCreatedAtDesc(city.trim(), PUBLIC_GAME_STATUSES);

        return games.stream()
                .map(this::buildGameListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GameResponse getOrganizerGameById(String organizerEmail, Long gameId) {
        return buildGameResponse(getOrganizerGame(organizerEmail, gameId));
    }

    @Transactional(readOnly = true)
    public List<IncomingGameRegistrationResponse> getIncomingRegistrations(String organizerEmail, Long gameId) {
        Game game = getOrganizerGame(organizerEmail, gameId);

        return gameRegistrationRepository.findAllByGameIdAndStatusOrderByCreatedAtDesc(
                        game.getId(),
                        GameRegistrationStatus.PENDING
                ).stream()
                .map(this::buildIncomingGameRegistrationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamGameRegistrationResponse> getTeamRegistrations(String captainEmail) {
        Team team = getCaptainTeam(captainEmail);

        return gameRegistrationRepository.findAllByTeamIdOrderByCreatedAtDesc(team.getId()).stream()
                .map(this::buildTeamGameRegistrationResponse)
                .toList();
    }

    private void validateCreateGameRequest(CreateGameRequest request) {
        validateGameRequest(
                request.minTeamSize(),
                request.maxTeamSize(),
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
        User captain = userRepository.findByEmail(captainEmail)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        TeamMembership membership = teamMembershipRepository.findByUserIdAndStatus(captain.getId(), TeamMembershipStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("У пользователя нет команды"));

        if (membership.getRole() != TeamMembershipRole.CAPTAIN) {
            throw new BadRequestException("Подавать заявку на игру может только капитан команды");
        }

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
            Instant registrationStartsAt,
            Instant registrationEndsAt,
            Instant startsAt
    ) {
        if (minTeamSize > maxTeamSize) {
            throw new BadRequestException("Минимальный размер команды не может быть больше максимального");
        }

        if (registrationStartsAt != null && registrationEndsAt != null && registrationStartsAt.isAfter(registrationEndsAt)) {
            throw new BadRequestException("Дата начала регистрации не может быть позже даты окончания регистрации");
        }

        if (registrationEndsAt != null && registrationEndsAt.isAfter(startsAt)) {
            throw new BadRequestException("Дата окончания регистрации не может быть позже даты начала игры");
        }
    }

    private void applyStatusTransition(Game game, GameStatus targetStatus) {
        GameStatus currentStatus = game.getStatus();

        if (currentStatus == targetStatus) {
            return;
        }

        boolean allowed = switch (currentStatus) {
            case DRAFT -> targetStatus == GameStatus.REGISTRATION_OPEN || targetStatus == GameStatus.CANCELED;
            case REGISTRATION_OPEN -> targetStatus == GameStatus.REGISTRATION_CLOSED || targetStatus == GameStatus.CANCELED;
            case REGISTRATION_CLOSED -> targetStatus == GameStatus.IN_PROGRESS || targetStatus == GameStatus.CANCELED;
            case IN_PROGRESS -> targetStatus == GameStatus.FINISHED;
            case FINISHED, CANCELED -> false;
        };

        if (!allowed) {
            throw new BadRequestException("Недопустимый переход статуса игры");
        }

        game.setStatus(targetStatus);

        if (targetStatus == GameStatus.FINISHED) {
            game.setFinishedAt(Instant.now());
        }

        if (targetStatus != GameStatus.FINISHED) {
            game.setFinishedAt(null);
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
}
