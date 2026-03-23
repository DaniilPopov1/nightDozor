package com.example.server.game.controller;

import com.example.server.game.dto.CreateGameRequest;
import com.example.server.game.dto.IncomingGameRegistrationResponse;
import com.example.server.game.dto.GameRegistrationResponse;
import com.example.server.game.dto.GameListItemResponse;
import com.example.server.game.dto.GameResponse;
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

    @GetMapping("/my/{gameId}/registrations")
    public ResponseEntity<List<IncomingGameRegistrationResponse>> getIncomingRegistrations(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long gameId
    ) {
        return ResponseEntity.ok(gameService.getIncomingRegistrations(userDetails.getUsername(), gameId));
    }
}
