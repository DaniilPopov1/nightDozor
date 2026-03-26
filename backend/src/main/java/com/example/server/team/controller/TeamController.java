package com.example.server.team.controller;

import com.example.server.team.dto.CreateTeamRequest;
import com.example.server.team.dto.IncomingJoinRequestResponse;
import com.example.server.team.dto.JoinTeamByCodeRequest;
import com.example.server.team.dto.OutgoingJoinRequestResponse;
import com.example.server.team.dto.TeamJoinRequestDecisionResponse;
import com.example.server.team.dto.TeamJoinRequestResponse;
import com.example.server.team.dto.TeamListItemResponse;
import com.example.server.team.dto.TeamResponse;
import com.example.server.team.dto.UpdateTeamRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
/**
 * REST-контроллер для управления командами, членством и заявками на вступление.
 */
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    /**
     * Создает новую команду для текущего участника.
     *
     * @param userDetails текущий аутентифицированный пользователь
     * @param request данные для создания команды
     * @return созданная команда
     */
    public ResponseEntity<TeamResponse> createTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateTeamRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createTeam(userDetails.getUsername(), request));
    }

    @PostMapping("/join-by-code")
    /**
     * Добавляет участника в команду по коду приглашения.
     *
     * @param userDetails текущий аутентифицированный пользователь
     * @param request код приглашения команды
     * @return актуальные данные команды
     */
    public ResponseEntity<TeamResponse> joinTeamByCode(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody JoinTeamByCodeRequest request
    ) {
        return ResponseEntity.ok(teamService.joinTeamByCode(userDetails.getUsername(), request));
    }

    @PostMapping("/{teamId}/join-requests")
    /**
     * Создает заявку пользователя на вступление в выбранную команду.
     *
     * @param userDetails текущий аутентифицированный пользователь
     * @param teamId идентификатор команды
     * @return созданная заявка
     */
    public ResponseEntity<TeamJoinRequestResponse> createJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createJoinRequest(userDetails.getUsername(), teamId));
    }

    @PostMapping("/{teamId}/join-requests/cancel")
    /**
     * Отменяет ранее отправленную заявку на вступление в команду.
     *
     * @param userDetails текущий аутентифицированный пользователь
     * @param teamId идентификатор команды
     * @return текстовое подтверждение отмены
     */
    public ResponseEntity<ApiMessageResponse> cancelJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId
    ) {
        teamService.cancelJoinRequest(userDetails.getUsername(), teamId);
        return ResponseEntity.ok(new ApiMessageResponse("Заявка на вступление отменена"));
    }

    @PostMapping("/{teamId}/join-requests/{userId}/approve")
    /**
     * Подтверждает заявку пользователя на вступление в команду.
     *
     * @param userDetails текущий капитан команды
     * @param teamId идентификатор команды
     * @param userId идентификатор пользователя-заявителя
     * @return результат обработки заявки
     */
    public ResponseEntity<TeamJoinRequestDecisionResponse> approveJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(teamService.approveJoinRequest(userDetails.getUsername(), teamId, userId));
    }

    @PostMapping("/{teamId}/join-requests/{userId}/reject")
    /**
     * Отклоняет заявку пользователя на вступление в команду.
     *
     * @param userDetails текущий капитан команды
     * @param teamId идентификатор команды
     * @param userId идентификатор пользователя-заявителя
     * @return результат обработки заявки
     */
    public ResponseEntity<TeamJoinRequestDecisionResponse> rejectJoinRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(teamService.rejectJoinRequest(userDetails.getUsername(), teamId, userId));
    }

    @PostMapping("/{teamId}/members/{userId}/remove")
    /**
     * Исключает активного участника из команды.
     *
     * @param userDetails текущий капитан команды
     * @param teamId идентификатор команды
     * @param userId идентификатор исключаемого участника
     * @return текстовое подтверждение исключения
     */
    public ResponseEntity<ApiMessageResponse> removeMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long userId
    ) {
        teamService.removeMember(userDetails.getUsername(), teamId, userId);
        return ResponseEntity.ok(new ApiMessageResponse("Участник исключен из команды"));
    }

    @PostMapping("/{teamId}/captain/{userId}/transfer")
    /**
     * Передает роль капитана другому активному участнику команды.
     *
     * @param userDetails текущий капитан
     * @param teamId идентификатор команды
     * @param userId идентификатор нового капитана
     * @return обновленные данные команды
     */
    public ResponseEntity<TeamResponse> transferCaptainRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(teamService.transferCaptainRole(userDetails.getUsername(), teamId, userId));
    }

    @PutMapping("/{teamId}")
    /**
     * Обновляет основные данные команды.
     *
     * @param userDetails текущий капитан команды
     * @param teamId идентификатор команды
     * @param request новые данные команды
     * @return обновленные данные команды
     */
    public ResponseEntity<TeamResponse> updateTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        return ResponseEntity.ok(teamService.updateTeam(userDetails.getUsername(), teamId, request));
    }

    @PostMapping("/{teamId}/disband")
    /**
     * Явно расформировывает команду.
     *
     * @param userDetails текущий капитан команды
     * @param teamId идентификатор команды
     * @return текстовое подтверждение расформирования
     */
    public ResponseEntity<ApiMessageResponse> disbandTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId
    ) {
        teamService.disbandTeam(userDetails.getUsername(), teamId);
        return ResponseEntity.ok(new ApiMessageResponse("Команда расформирована"));
    }

    @PostMapping("/leave")
    /**
     * Позволяет текущему участнику выйти из своей команды.
     *
     * @param userDetails текущий пользователь
     * @return текстовое подтверждение выхода
     */
    public ResponseEntity<ApiMessageResponse> leaveTeam(@AuthenticationPrincipal UserDetails userDetails) {
        teamService.leaveTeam(userDetails.getUsername());
        return ResponseEntity.ok(new ApiMessageResponse("Выход из команды выполнен"));
    }

    @GetMapping
    /**
     * Возвращает список команд с опциональной фильтрацией по городу.
     *
     * @param city город для фильтрации
     * @return список команд
     */
    public ResponseEntity<List<TeamListItemResponse>> getTeams(
            @RequestParam(required = false) String city
    ) {
        return ResponseEntity.ok(teamService.getTeams(city));
    }

    @GetMapping("/{teamId}")
    /**
     * Возвращает подробную информацию о команде по идентификатору.
     *
     * @param teamId идентификатор команды
     * @return данные команды
     */
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.getTeamById(teamId));
    }

    @GetMapping("/me/join-requests")
    /**
     * Возвращает список входящих заявок в команду текущего капитана.
     *
     * @param userDetails текущий капитан команды
     * @return список входящих заявок
     */
    public ResponseEntity<List<IncomingJoinRequestResponse>> getIncomingJoinRequests(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(teamService.getIncomingJoinRequests(userDetails.getUsername()));
    }

    @GetMapping("/me/outgoing-join-requests")
    /**
     * Возвращает список исходящих заявок текущего пользователя в другие команды.
     *
     * @param userDetails текущий пользователь
     * @return список исходящих заявок
     */
    public ResponseEntity<List<OutgoingJoinRequestResponse>> getOutgoingJoinRequests(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(teamService.getOutgoingJoinRequests(userDetails.getUsername()));
    }

    @GetMapping("/me")
    /**
     * Возвращает команду текущего пользователя.
     *
     * @param userDetails текущий пользователь
     * @return данные команды пользователя
     */
    public ResponseEntity<TeamResponse> getCurrentTeam(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(teamService.getCurrentTeam(userDetails.getUsername()));
    }
}
