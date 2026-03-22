package com.example.server.game.controller;

import com.example.server.game.dto.CreateGameRequest;
import com.example.server.game.dto.GameListItemResponse;
import com.example.server.game.dto.GameResponse;
import com.example.server.game.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
