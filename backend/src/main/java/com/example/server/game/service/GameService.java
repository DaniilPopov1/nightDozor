package com.example.server.game.service;

import com.example.server.auth.entity.Role;
import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import com.example.server.common.exception.BadRequestException;
import com.example.server.common.exception.NotFoundException;
import com.example.server.game.dto.CreateGameRequest;
import com.example.server.game.dto.GameListItemResponse;
import com.example.server.game.dto.GameResponse;
import com.example.server.game.entity.Game;
import com.example.server.game.entity.GameStatus;
import com.example.server.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        User organizer = getOrganizerByEmail(organizerEmail);

        Game game = gameRepository.findByIdAndOrganizerId(gameId, organizer.getId())
                .orElseThrow(() -> new NotFoundException("Игра не найдена"));

        return buildGameResponse(game);
    }

    private void validateCreateGameRequest(CreateGameRequest request) {
        if (request.minTeamSize() > request.maxTeamSize()) {
            throw new BadRequestException("Минимальный размер команды не может быть больше максимального");
        }

        if (request.registrationStartsAt() != null && request.registrationEndsAt() != null
                && request.registrationStartsAt().isAfter(request.registrationEndsAt())) {
            throw new BadRequestException("Дата начала регистрации не может быть позже даты окончания регистрации");
        }

        if (request.registrationEndsAt() != null && request.registrationEndsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("Дата окончания регистрации не может быть позже даты начала игры");
        }
    }

    private User getOrganizerByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (user.getRole() != Role.ORGANIZER) {
            throw new BadRequestException("Управлять играми могут только организаторы");
        }

        return user;
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
}
