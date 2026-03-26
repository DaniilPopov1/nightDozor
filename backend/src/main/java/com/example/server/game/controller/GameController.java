package com.example.server.game.controller;

import com.example.server.game.dto.CreateGameRequest;
import com.example.server.game.dto.CreateGameTaskHintRequest;
import com.example.server.game.dto.CreateGameTaskRequest;
import com.example.server.game.dto.CreateTeamGameRouteRequest;
import com.example.server.game.dto.CurrentGameTaskResponse;
import com.example.server.game.dto.GameTeamProgressResponse;
import com.example.server.game.dto.IncomingGameRegistrationResponse;
import com.example.server.game.dto.GameStartResponse;
import com.example.server.game.dto.GameRegistrationResponse;
import com.example.server.game.dto.GameListItemResponse;
import com.example.server.game.dto.GameResponse;
import com.example.server.game.dto.GameTaskHintResponse;
import com.example.server.game.dto.GameTaskResponse;
import com.example.server.game.dto.AddTaskToRouteRequest;
import com.example.server.game.dto.SubmitTaskKeyRequest;
import com.example.server.game.dto.SubmitTaskKeyResponse;
import com.example.server.game.dto.TeamGameRouteResponse;
import com.example.server.game.dto.TeamGameRegistrationResponse;
import com.example.server.game.dto.UpdateGameRequest;
import com.example.server.game.dto.UpdateGameStatusRequest;
import com.example.server.game.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
/**
 * REST-контроллер для управления играми, заданиями, маршрутами и игровым прогрессом.
 */
public class GameController {

    private final GameService gameService;

    @PostMapping
    /**
     * Создает новую игру от имени текущего организатора.
     *
     * @param userDetails текущий аутентифицированный пользователь
     * @param request данные для создания игры
     * @return созданная игра
     */
    public ResponseEntity<GameResponse> createGame(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateGameRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameService.createGame(userDetails.getUsername(), request));
    }

    @PostMapping("/{gameId}/registrations")
    /**
     * Подает заявку команды текущего капитана на участие в игре.
     *
     * @param userDetails текущий капитан команды
     * @param gameId идентификатор игры
     * @return созданная заявка на участие
     */
    public ResponseEntity<GameRegistrationResponse> submitGameRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameService.submitGameRegistration(userDetails.getUsername(), gameId));
    }

    @PostMapping("/{gameId}/registrations/cancel")
    /**
     * Отменяет pending-заявку команды на участие в игре.
     *
     * @param userDetails текущий капитан команды
     * @param gameId идентификатор игры
     * @return обновленная заявка
     */
    public ResponseEntity<GameRegistrationResponse> cancelGameRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.ok(gameService.cancelGameRegistration(userDetails.getUsername(), gameId));
    }

    @GetMapping
    /**
     * Возвращает публичный список игр с опциональной фильтрацией по городу.
     *
     * @param city город для фильтрации
     * @return список публичных игр
     */
    public ResponseEntity<List<GameListItemResponse>> getPublicGames(
            @RequestParam(required = false) String city
    ) {
        return ResponseEntity.ok(gameService.getPublicGames(city));
    }

    @GetMapping("/my")
    /**
     * Возвращает список игр текущего организатора.
     *
     * @param userDetails текущий организатор
     * @return список игр организатора
     */
    public ResponseEntity<List<GameListItemResponse>> getMyGames(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(gameService.getOrganizerGames(userDetails.getUsername()));
    }

    @GetMapping("/my/{gameId}")
    /**
     * Возвращает подробную информацию об игре текущего организатора.
     *
     * @param userDetails текущий организатор
     * @param gameId идентификатор игры
     * @return данные игры
     */
    public ResponseEntity<GameResponse> getMyGameById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.ok(gameService.getOrganizerGameById(userDetails.getUsername(), gameId));
    }

    @PutMapping("/my/{gameId}")
    /**
     * Обновляет параметры игры до ее старта.
     *
     * @param userDetails текущий организатор
     * @param gameId идентификатор игры
     * @param request новые данные игры
     * @return обновленная игра
     */
    public ResponseEntity<GameResponse> updateMyGame(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @Valid @RequestBody UpdateGameRequest request
    ) {
        return ResponseEntity.ok(gameService.updateGame(userDetails.getUsername(), gameId, request));
    }

    @PostMapping("/my/{gameId}/status")
    /**
     * Меняет статус игры в рамках допустимого жизненного цикла.
     *
     * @param userDetails текущий организатор
     * @param gameId идентификатор игры
     * @param request новый статус игры
     * @return обновленная игра
     */
    public ResponseEntity<GameResponse> updateMyGameStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @Valid @RequestBody UpdateGameStatusRequest request
    ) {
        return ResponseEntity.ok(gameService.updateGameStatus(userDetails.getUsername(), gameId, request));
    }

    @PostMapping("/my/{gameId}/start")
    /**
     * Запускает игру и создает игровые сессии для подтвержденных команд.
     *
     * @param userDetails текущий организатор
     * @param gameId идентификатор игры
     * @return информация о запуске игры
     */
    public ResponseEntity<GameStartResponse> startGame(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.ok(gameService.startGame(userDetails.getUsername(), gameId));
    }

    @PostMapping("/my/{gameId}/tasks")
    /**
     * Создает новое задание внутри игры.
     *
     * @param userDetails текущий организатор
     * @param gameId идентификатор игры
     * @param request данные задания
     * @return созданное задание
     */
    public ResponseEntity<GameTaskResponse> createTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @Valid @RequestBody CreateGameTaskRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameService.createTask(userDetails.getUsername(), gameId, request));
    }

    @PostMapping("/my/{gameId}/tasks/{taskId}/hints")
    /**
     * Добавляет подсказку к существующему заданию игры.
     *
     * @param userDetails текущий организатор
     * @param gameId идентификатор игры
     * @param taskId идентификатор задания
     * @param request данные подсказки
     * @return созданная подсказка
     */
    public ResponseEntity<GameTaskHintResponse> addTaskHint(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @PathVariable Long taskId,
            @Valid @RequestBody CreateGameTaskHintRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameService.addTaskHint(userDetails.getUsername(), gameId, taskId, request));
    }

    @PostMapping("/my/{gameId}/routes")
    /**
     * Создает маршрут прохождения заданий для конкретной команды.
     *
     * @param userDetails текущий организатор
     * @param gameId идентификатор игры
     * @param request данные маршрута
     * @return созданный маршрут
     */
    public ResponseEntity<TeamGameRouteResponse> createRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @Valid @RequestBody CreateTeamGameRouteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameService.createRoute(userDetails.getUsername(), gameId, request));
    }

    @PostMapping("/my/{gameId}/routes/{routeId}/items")
    /**
     * Добавляет задание в маршрут команды.
     *
     * @param userDetails текущий организатор
     * @param gameId идентификатор игры
     * @param routeId идентификатор маршрута
     * @param request данные элемента маршрута
     * @return обновленный маршрут
     */
    public ResponseEntity<TeamGameRouteResponse> addTaskToRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @PathVariable Long routeId,
            @Valid @RequestBody AddTaskToRouteRequest request
    ) {
        return ResponseEntity.ok(gameService.addTaskToRoute(userDetails.getUsername(), gameId, routeId, request));
    }

    @GetMapping("/my/{gameId}/registrations")
    /**
     * Возвращает входящие заявки команд на участие в игре организатора.
     *
     * @param userDetails текущий организатор
     * @param gameId идентификатор игры
     * @return список заявок команд
     */
    public ResponseEntity<List<IncomingGameRegistrationResponse>> getIncomingRegistrations(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.ok(gameService.getIncomingRegistrations(userDetails.getUsername(), gameId));
    }

    @PostMapping("/my/{gameId}/registrations/{registrationId}/approve")
    /**
     * Подтверждает заявку команды на участие в игре.
     *
     * @param userDetails текущий организатор
     * @param gameId идентификатор игры
     * @param registrationId идентификатор заявки
     * @return обновленная заявка
     */
    public ResponseEntity<GameRegistrationResponse> approveRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @PathVariable Long registrationId
    ) {
        return ResponseEntity.ok(gameService.approveRegistration(userDetails.getUsername(), gameId, registrationId));
    }

    @PostMapping("/my/{gameId}/registrations/{registrationId}/reject")
    /**
     * Отклоняет заявку команды на участие в игре.
     *
     * @param userDetails текущий организатор
     * @param gameId идентификатор игры
     * @param registrationId идентификатор заявки
     * @return обновленная заявка
     */
    public ResponseEntity<GameRegistrationResponse> rejectRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @PathVariable Long registrationId
    ) {
        return ResponseEntity.ok(gameService.rejectRegistration(userDetails.getUsername(), gameId, registrationId));
    }

    @GetMapping("/registrations/my-team")
    /**
     * Возвращает список заявок текущей команды на разные игры.
     *
     * @param userDetails текущий капитан команды
     * @return список заявок команды
     */
    public ResponseEntity<List<TeamGameRegistrationResponse>> getMyTeamRegistrations(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(gameService.getTeamRegistrations(userDetails.getUsername()));
    }

    @GetMapping("/current-task")
    /**
     * Возвращает текущее активное задание команды пользователя.
     *
     * @param userDetails текущий участник команды
     * @return данные текущего задания
     */
    public ResponseEntity<CurrentGameTaskResponse> getCurrentTask(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(gameService.getCurrentTask(userDetails.getUsername()));
    }

    @PostMapping("/current-task/submit-key")
    /**
     * Отправляет ключ текущего задания от имени капитана команды.
     *
     * @param userDetails текущий капитан команды
     * @param request ключ задания
     * @return результат проверки ключа
     */
    public ResponseEntity<SubmitTaskKeyResponse> submitTaskKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SubmitTaskKeyRequest request
    ) {
        return ResponseEntity.ok(gameService.submitTaskKey(userDetails.getUsername(), request));
    }

    @GetMapping("/progress/my-team")
    /**
     * Возвращает текущий прогресс и место команды в общем зачете игры.
     *
     * @param userDetails текущий участник команды
     * @return прогресс команды и standings игры
     */
    public ResponseEntity<GameTeamProgressResponse> getMyTeamProgress(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(gameService.getMyTeamProgress(userDetails.getUsername()));
    }
}
