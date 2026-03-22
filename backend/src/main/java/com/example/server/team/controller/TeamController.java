package com.example.server.team.controller;

import com.example.server.team.dto.CreateTeamRequest;
import com.example.server.team.dto.IncomingJoinRequestResponse;
import com.example.server.team.dto.JoinTeamByCodeRequest;
import com.example.server.team.dto.OutgoingJoinRequestResponse;
import com.example.server.team.dto.TeamJoinRequestDecisionResponse;
import com.example.server.team.dto.TeamJoinRequestResponse;
import com.example.server.team.dto.TeamListItemResponse;
import com.example.server.team.dto.TeamResponse;
import com.example.server.common.response.ApiMessageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PostMapping("/{teamId}/join-requests/cancel")
    public ResponseEntity<ApiMessageResponse> cancelJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId
    ) {
        teamService.cancelJoinRequest(userDetails.getUsername(), teamId);
        return ResponseEntity.ok(new ApiMessageResponse("Заявка на вступление отменена"));
    }

    @PostMapping("/{teamId}/join-requests/{userId}/approve")
    public ResponseEntity<TeamJoinRequestDecisionResponse> approveJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(teamService.approveJoinRequest(userDetails.getUsername(), teamId, userId));
    }

    @PostMapping("/{teamId}/join-requests/{userId}/reject")
    public ResponseEntity<TeamJoinRequestDecisionResponse> rejectJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(teamService.rejectJoinRequest(userDetails.getUsername(), teamId, userId));
    }

    @PostMapping("/{teamId}/members/{userId}/remove")
    public ResponseEntity<ApiMessageResponse> removeMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long userId
    ) {
        teamService.removeMember(userDetails.getUsername(), teamId, userId);
        return ResponseEntity.ok(new ApiMessageResponse("Участник исключен из команды"));
    }

    @PostMapping("/{teamId}/captain/{userId}/transfer")
    public ResponseEntity<TeamResponse> transferCaptainRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(teamService.transferCaptainRole(userDetails.getUsername(), teamId, userId));
    }

    @PostMapping("/leave")
    public ResponseEntity<ApiMessageResponse> leaveTeam(@AuthenticationPrincipal UserDetails userDetails) {
        teamService.leaveTeam(userDetails.getUsername());
        return ResponseEntity.ok(new ApiMessageResponse("Выход из команды выполнен"));
    }

    @GetMapping
    public ResponseEntity<List<TeamListItemResponse>> getTeams(
            @RequestParam(required = false) String city
    ) {
        return ResponseEntity.ok(teamService.getTeams(city));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeamById(teamId));
    }

    @GetMapping("/me/join-requests")
    public ResponseEntity<List<IncomingJoinRequestResponse>> getIncomingJoinRequests(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(teamService.getIncomingJoinRequests(userDetails.getUsername()));
    }

    @GetMapping("/me/outgoing-join-requests")
    public ResponseEntity<List<OutgoingJoinRequestResponse>> getOutgoingJoinRequests(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(teamService.getOutgoingJoinRequests(userDetails.getUsername()));
    }

    @GetMapping("/me")
    public ResponseEntity<TeamResponse> getCurrentTeam(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teamService.getCurrentTeam(userDetails.getUsername()));
    }
}
