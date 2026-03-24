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
public class GameController {

    private final GameService gameService;

    @PostMapping
    public ResponseEntity<GameResponse> createGame(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateGameRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameService.createGame(userDetails.getUsername(), request));
    }

    @PostMapping("/{gameId}/registrations")
    public ResponseEntity<GameRegistrationResponse> submitGameRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameService.submitGameRegistration(userDetails.getUsername(), gameId));
    }

    @PostMapping("/{gameId}/registrations/cancel")
    public ResponseEntity<GameRegistrationResponse> cancelGameRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.ok(gameService.cancelGameRegistration(userDetails.getUsername(), gameId));
    }

    @GetMapping
    public ResponseEntity<List<GameListItemResponse>> getPublicGames(
            @RequestParam(required = false) String city
    ) {
        return ResponseEntity.ok(gameService.getPublicGames(city));
    }

    @GetMapping("/my")
    public ResponseEntity<List<GameListItemResponse>> getMyGames(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(gameService.getOrganizerGames(userDetails.getUsername()));
    }

    @GetMapping("/my/{gameId}")
    public ResponseEntity<GameResponse> getMyGameById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.ok(gameService.getOrganizerGameById(userDetails.getUsername(), gameId));
    }

    @PutMapping("/my/{gameId}")
    public ResponseEntity<GameResponse> updateMyGame(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @Valid @RequestBody UpdateGameRequest request
    ) {
        return ResponseEntity.ok(gameService.updateGame(userDetails.getUsername(), gameId, request));
    }

    @PostMapping("/my/{gameId}/status")
    public ResponseEntity<GameResponse> updateMyGameStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @Valid @RequestBody UpdateGameStatusRequest request
    ) {
        return ResponseEntity.ok(gameService.updateGameStatus(userDetails.getUsername(), gameId, request));
    }

    @PostMapping("/my/{gameId}/start")
    public ResponseEntity<GameStartResponse> startGame(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.ok(gameService.startGame(userDetails.getUsername(), gameId));
    }

    @PostMapping("/my/{gameId}/tasks")
    public ResponseEntity<GameTaskResponse> createTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @Valid @RequestBody CreateGameTaskRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameService.createTask(userDetails.getUsername(), gameId, request));
    }

    @PostMapping("/my/{gameId}/tasks/{taskId}/hints")
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
    public ResponseEntity<TeamGameRouteResponse> createRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @Valid @RequestBody CreateTeamGameRouteRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameService.createRoute(userDetails.getUsername(), gameId, request));
    }

    @PostMapping("/my/{gameId}/routes/{routeId}/items")
    public ResponseEntity<TeamGameRouteResponse> addTaskToRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @PathVariable Long routeId,
            @Valid @RequestBody AddTaskToRouteRequest request
    ) {
        return ResponseEntity.ok(gameService.addTaskToRoute(userDetails.getUsername(), gameId, routeId, request));
    }

    @GetMapping("/my/{gameId}/registrations")
    public ResponseEntity<List<IncomingGameRegistrationResponse>> getIncomingRegistrations(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.ok(gameService.getIncomingRegistrations(userDetails.getUsername(), gameId));
    }

    @PostMapping("/my/{gameId}/registrations/{registrationId}/approve")
    public ResponseEntity<GameRegistrationResponse> approveRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @PathVariable Long registrationId
    ) {
        return ResponseEntity.ok(gameService.approveRegistration(userDetails.getUsername(), gameId, registrationId));
    }

    @PostMapping("/my/{gameId}/registrations/{registrationId}/reject")
    public ResponseEntity<GameRegistrationResponse> rejectRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId,
            @PathVariable Long registrationId
    ) {
        return ResponseEntity.ok(gameService.rejectRegistration(userDetails.getUsername(), gameId, registrationId));
    }

    @GetMapping("/registrations/my-team")
    public ResponseEntity<List<TeamGameRegistrationResponse>> getMyTeamRegistrations(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(gameService.getTeamRegistrations(userDetails.getUsername()));
    }

    @GetMapping("/current-task")
    public ResponseEntity<CurrentGameTaskResponse> getCurrentTask(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(gameService.getCurrentTask(userDetails.getUsername()));
    }

    @PostMapping("/current-task/submit-key")
    public ResponseEntity<SubmitTaskKeyResponse> submitTaskKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SubmitTaskKeyRequest request
    ) {
        return ResponseEntity.ok(gameService.submitTaskKey(userDetails.getUsername(), request));
    }

    @GetMapping("/progress/my-team")
    public ResponseEntity<GameTeamProgressResponse> getMyTeamProgress(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(gameService.getMyTeamProgress(userDetails.getUsername()));
    }
}
