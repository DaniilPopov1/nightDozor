package com.example.server.team.controller;

import com.example.server.team.dto.CreateTeamRequest;
import com.example.server.team.dto.JoinTeamByCodeRequest;
import com.example.server.team.dto.TeamJoinRequestResponse;
import com.example.server.team.dto.TeamResponse;
import com.example.server.team.service.TeamService;
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

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateTeamRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createTeam(userDetails.getUsername(), request));
    }

    @PostMapping("/join-by-code")
    public ResponseEntity<TeamResponse> joinTeamByCode(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JoinTeamByCodeRequest request
    ) {
        return ResponseEntity.ok(teamService.joinTeamByCode(userDetails.getUsername(), request));
    }

    @PostMapping("/{teamId}/join-requests")
    public ResponseEntity<TeamJoinRequestResponse> createJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createJoinRequest(userDetails.getUsername(), teamId));
    }

    @GetMapping("/me")
    public ResponseEntity<TeamResponse> getCurrentTeam(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teamService.getCurrentTeam(userDetails.getUsername()));
    }
}
