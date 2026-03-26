package com.example.server.team.service;

import com.example.server.auth.entity.Role;
import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import com.example.server.common.exception.BadRequestException;
import com.example.server.common.exception.ConflictException;
import com.example.server.common.exception.NotFoundException;
import com.example.server.team.dto.CreateTeamRequest;
import com.example.server.team.dto.IncomingJoinRequestResponse;
import com.example.server.team.dto.JoinTeamByCodeRequest;
import com.example.server.team.dto.OutgoingJoinRequestResponse;
import com.example.server.team.dto.TeamJoinRequestDecisionResponse;
import com.example.server.team.dto.TeamMemberResponse;
import com.example.server.team.dto.TeamJoinRequestResponse;
import com.example.server.team.dto.TeamListItemResponse;
import com.example.server.team.dto.TeamResponse;
import com.example.server.team.dto.UpdateTeamRequest;
import com.example.server.team.entity.Team;
import com.example.server.team.entity.TeamMembership;
import com.example.server.team.entity.TeamMembershipRole;
import com.example.server.team.entity.TeamMembershipStatus;
import com.example.server.team.repository.TeamMembershipRepository;
import com.example.server.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
/**
 * Сервис бизнес-логики команд, членства и заявок на вступление.
 */
public class TeamService {

    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 8;

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final UserRepository userRepository;

    @Transactional
    /**
     * Создает новую команду и автоматически добавляет создателя как капитана.
     *
     * @param email email текущего пользователя
     * @param request данные для создания команды
     * @return созданная команда
     */
    public TeamResponse createTeam(String email, CreateTeamRequest request) {
        User user = getUserByEmail(email);
        validateCanCreateTeam(user);

        Team team = new Team();
        team.setName(request.name().trim());
        team.setCity(request.city().trim());
        team.setCaptain(user);
        team.setInviteCode(generateUniqueInviteCode());

        Team savedTeam = teamRepository.save(team);

        TeamMembership membership = new TeamMembership();
        membership.setTeam(savedTeam);
        membership.setUser(user);
        membership.setRole(TeamMembershipRole.CAPTAIN);
        membership.setStatus(TeamMembershipStatus.ACTIVE);
        membership.setJoinedAt(Instant.now());

        teamMembershipRepository.save(membership);

        return buildTeamResponse(savedTeam);
    }

    @Transactional
    /**
     * Добавляет пользователя в команду по invite-коду.
     *
     * @param email email текущего пользователя
     * @param request запрос с invite-кодом
     * @return данные команды после вступления
     */
    public TeamResponse joinTeamByCode(String email, JoinTeamByCodeRequest request) {
        User user = getUserByEmail(email);
        validateCanJoinTeam(user);

        String inviteCode = normalizeInviteCode(request.inviteCode());
        Team team = teamRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new NotFoundException("Команда с таким кодом приглашения не найдена"));

        if (team.getCaptain().getId().equals(user.getId())) {
            throw new ConflictException("Капитан уже состоит в этой команде");
        }

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                .orElseGet(TeamMembership::new);

        membership.setTeam(team);
        membership.setUser(user);
        membership.setRole(TeamMembershipRole.MEMBER);
        membership.setStatus(TeamMembershipStatus.ACTIVE);
        membership.setJoinedAt(Instant.now());

        teamMembershipRepository.save(membership);

        return buildTeamResponse(team);
    }

    @Transactional
    /**
     * Создает заявку на вступление пользователя в команду.
     *
     * @param email email текущего пользователя
     * @param teamId идентификатор команды
     * @return созданная заявка
     */
    public TeamJoinRequestResponse createJoinRequest(String email, Long teamId) {
        User user = getUserByEmail(email);
        validateCanRequestToJoinTeam(user);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        if (team.getCaptain().getId().equals(user.getId())) {
            throw new ConflictException("Капитан уже состоит в этой команде");
        }

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                .orElseGet(TeamMembership::new);

        if (membership.getId() != null && membership.getStatus() == TeamMembershipStatus.PENDING) {
            throw new ConflictException("Заявка в эту команду уже отправлена");
        }

        membership.setTeam(team);
        membership.setUser(user);
        membership.setRole(TeamMembershipRole.MEMBER);
        membership.setStatus(TeamMembershipStatus.PENDING);
        membership.setJoinedAt(null);

        TeamMembership savedMembership = teamMembershipRepository.save(membership);

        return new TeamJoinRequestResponse(
                team.getId(),
                team.getName(),
                team.getCity(),
                savedMembership.getStatus(),
                savedMembership.getCreatedAt()
        );
    }

    @Transactional
    /**
     * Отменяет pending-заявку пользователя на вступление в команду.
     *
     * @param email email текущего пользователя
     * @param teamId идентификатор команды
     */
    public void cancelJoinRequest(String email, Long teamId) {
        User user = getUserByEmail(email);

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new NotFoundException("Заявка не найдена"));

        if (membership.getStatus() != TeamMembershipStatus.PENDING) {
            throw new BadRequestException("Можно отменить только заявку в статусе PENDING");
        }

        membership.setStatus(TeamMembershipStatus.LEFT);
        membership.setJoinedAt(null);
        teamMembershipRepository.save(membership);
    }

    @Transactional
    /**
     * Подтверждает заявку пользователя на вступление в команду.
     *
     * @param captainEmail email капитана команды
     * @param teamId идентификатор команды
     * @param userId идентификатор пользователя-заявителя
     * @return результат обработки заявки
     */
    public TeamJoinRequestDecisionResponse approveJoinRequest(String captainEmail, Long teamId, Long userId) {
        resolveCaptainMembership(captainEmail, teamId);
        TeamMembership membership = getPendingMembership(teamId, userId);

        if (teamMembershipRepository.existsByUserIdAndStatus(userId, TeamMembershipStatus.ACTIVE)) {
            throw new ConflictException("Пользователь уже состоит в команде");
        }

        membership.setStatus(TeamMembershipStatus.ACTIVE);
        membership.setJoinedAt(Instant.now());

        TeamMembership savedMembership = teamMembershipRepository.save(membership);
        return buildJoinRequestDecisionResponse(savedMembership);
    }

    @Transactional
    /**
     * Отклоняет заявку пользователя на вступление в команду.
     *
     * @param captainEmail email капитана команды
     * @param teamId идентификатор команды
     * @param userId идентификатор пользователя-заявителя
     * @return результат обработки заявки
     */
    public TeamJoinRequestDecisionResponse rejectJoinRequest(String captainEmail, Long teamId, Long userId) {
        resolveCaptainMembership(captainEmail, teamId);
        TeamMembership membership = getPendingMembership(teamId, userId);

        membership.setStatus(TeamMembershipStatus.REJECTED);
        membership.setJoinedAt(null);

        TeamMembership savedMembership = teamMembershipRepository.save(membership);
        return buildJoinRequestDecisionResponse(savedMembership);
    }

    @Transactional
    /**
     * Исключает активного участника из команды.
     *
     * @param captainEmail email капитана команды
     * @param teamId идентификатор команды
     * @param userId идентификатор исключаемого участника
     */
    public void removeMember(String captainEmail, Long teamId, Long userId) {
        TeamMembership captainMembership = resolveCaptainMembership(captainEmail, teamId);

        if (captainMembership.getUser().getId().equals(userId)) {
            throw new BadRequestException("Капитан не может исключить сам себя");
        }

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new NotFoundException("Участник команды не найден"));

        if (membership.getStatus() != TeamMembershipStatus.ACTIVE) {
            throw new BadRequestException("Можно исключить только активного участника команды");
        }

        if (membership.getRole() == TeamMembershipRole.CAPTAIN) {
            throw new BadRequestException("Нельзя исключить капитана команды");
        }

        membership.setStatus(TeamMembershipStatus.LEFT);
        membership.setJoinedAt(null);
        teamMembershipRepository.save(membership);
    }

    @Transactional
    /**
     * Передает роль капитана другому активному участнику команды.
     *
     * @param currentCaptainEmail email текущего капитана
     * @param teamId идентификатор команды
     * @param newCaptainUserId идентификатор нового капитана
     * @return обновленные данные команды
     */
    public TeamResponse transferCaptainRole(String currentCaptainEmail, Long teamId, Long newCaptainUserId) {
        TeamMembership currentCaptainMembership = resolveCaptainMembership(currentCaptainEmail, teamId);

        if (currentCaptainMembership.getUser().getId().equals(newCaptainUserId)) {
            throw new BadRequestException("Новый капитан должен отличаться от текущего");
        }

        TeamMembership newCaptainMembership = teamMembershipRepository.findByTeamIdAndUserId(teamId, newCaptainUserId)
                .orElseThrow(() -> new NotFoundException("Участник команды не найден"));

        if (newCaptainMembership.getStatus() != TeamMembershipStatus.ACTIVE) {
            throw new BadRequestException("Капитаном можно назначить только активного участника команды");
        }

        currentCaptainMembership.setRole(TeamMembershipRole.MEMBER);
        newCaptainMembership.setRole(TeamMembershipRole.CAPTAIN);

        Team team = currentCaptainMembership.getTeam();
        team.setCaptain(newCaptainMembership.getUser());

        teamMembershipRepository.save(currentCaptainMembership);
        teamMembershipRepository.save(newCaptainMembership);
        teamRepository.save(team);

        return buildTeamResponse(team);
    }

    @Transactional
    /**
     * Обновляет название, город и при необходимости invite-код команды.
     *
     * @param captainEmail email капитана команды
     * @param teamId идентификатор команды
     * @param request новые данные команды
     * @return обновленные данные команды
     */
    public TeamResponse updateTeam(String captainEmail, Long teamId, UpdateTeamRequest request) {
        TeamMembership captainMembership = resolveCaptainMembership(captainEmail, teamId);
        Team team = captainMembership.getTeam();

        team.setName(request.name().trim());
        team.setCity(request.city().trim());

        if (request.regenerateInviteCode()) {
            team.setInviteCode(generateUniqueInviteCode());
        }

        Team savedTeam = teamRepository.save(team);
        return buildTeamResponse(savedTeam);
    }

    @Transactional
    /**
     * Удаляет команду и все связанные записи членства.
     *
     * @param captainEmail email капитана команды
     * @param teamId идентификатор команды
     */
    public void disbandTeam(String captainEmail, Long teamId) {
        TeamMembership captainMembership = resolveCaptainMembership(captainEmail, teamId);
        Team team = captainMembership.getTeam();

        List<TeamMembership> memberships = teamMembershipRepository.findAllByTeamId(team.getId());
        teamMembershipRepository.deleteAll(memberships);
        teamRepository.delete(team);
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает команду текущего пользователя.
     *
     * @param email email пользователя
     * @return данные команды пользователя
     */
    public TeamResponse getCurrentTeam(String email) {
        User user = getUserByEmail(email);

        TeamMembership membership = teamMembershipRepository.findByUserIdAndStatus(
                        user.getId(),
                        TeamMembershipStatus.ACTIVE
                )
                .orElseThrow(() -> new NotFoundException("У пользователя нет команды"));

        return buildTeamResponse(membership.getTeam());
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает список команд с опциональной фильтрацией по городу.
     *
     * @param city город для фильтрации
     * @return список команд
     */
    public List<TeamListItemResponse> getTeams(String city) {
        List<Team> teams = city == null || city.isBlank()
                ? teamRepository.findAllByOrderByCreatedAtDesc()
                : teamRepository.findAllByCityIgnoreCaseOrderByCreatedAtDesc(city.trim());

        return teams.stream()
                .map(this::buildTeamListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает подробную информацию о команде по идентификатору.
     *
     * @param teamId идентификатор команды
     * @return данные команды
     */
    public TeamResponse getTeamById(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        return buildTeamResponse(team);
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает список входящих pending-заявок в команду текущего капитана.
     *
     * @param captainEmail email капитана команды
     * @return список входящих заявок
     */
    public List<IncomingJoinRequestResponse> getIncomingJoinRequests(String captainEmail) {
        TeamMembership captainMembership = resolveCaptainMembership(captainEmail);

        return teamMembershipRepository.findAllByTeamIdAndStatus(
                        captainMembership.getTeam().getId(),
                        TeamMembershipStatus.PENDING
                ).stream()
                .map(this::buildIncomingJoinRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Возвращает список исходящих pending-заявок текущего пользователя.
     *
     * @param userEmail email пользователя
     * @return список исходящих заявок
     */
    public List<OutgoingJoinRequestResponse> getOutgoingJoinRequests(String userEmail) {
        User user = getUserByEmail(userEmail);

        return teamMembershipRepository.findAllByUserIdAndStatus(user.getId(), TeamMembershipStatus.PENDING).stream()
                .map(this::buildOutgoingJoinRequestResponse)
                .toList();
    }

    @Transactional
    /**
     * Позволяет активному участнику покинуть свою команду.
     *
     * @param email email пользователя
     */
    public void leaveTeam(String email) {
        User user = getUserByEmail(email);
        TeamMembership membership = teamMembershipRepository.findByUserIdAndStatus(user.getId(), TeamMembershipStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("У пользователя нет команды"));

        if (membership.getRole() == TeamMembershipRole.CAPTAIN) {
            throw new BadRequestException("Капитан не может выйти из команды без передачи роли капитана или расформирования команды");
        }

        membership.setStatus(TeamMembershipStatus.LEFT);
        membership.setJoinedAt(null);
        teamMembershipRepository.save(membership);
    }

    /**
     * Проверяет, что пользователь может создать новую команду.
     *
     * @param user пользователь, инициирующий создание команды
     */
    private void validateCanCreateTeam(User user) {
        if (user.getRole() != Role.PARTICIPANT) {
            throw new BadRequestException("Создавать команды могут только участники");
        }

        validateNoBlockingMemberships(user);
    }

    /**
     * Проверяет, что пользователь может вступить в команду по коду приглашения.
     *
     * @param user пользователь, инициирующий вступление
     */
    private void validateCanJoinTeam(User user) {
        if (user.getRole() != Role.PARTICIPANT) {
            throw new BadRequestException("Вступать в команды могут только участники");
        }

        validateNoBlockingMemberships(user);
    }

    /**
     * Проверяет, что пользователь может отправить заявку на вступление в команду.
     *
     * @param user пользователь, отправляющий заявку
     */
    private void validateCanRequestToJoinTeam(User user) {
        if (user.getRole() != Role.PARTICIPANT) {
            throw new BadRequestException("Отправлять заявки в команды могут только участники");
        }

        validateNoBlockingMemberships(user);
    }

    /**
     * Проверяет, что у пользователя нет активного членства или необработанных заявок.
     *
     * @param user пользователь для проверки
     */
    private void validateNoBlockingMemberships(User user) {
        if (teamMembershipRepository.existsByUserIdAndStatus(user.getId(), TeamMembershipStatus.ACTIVE)) {
            throw new ConflictException("Пользователь уже состоит в команде");
        }

        if (!teamMembershipRepository.findAllByUserIdAndStatus(user.getId(), TeamMembershipStatus.PENDING).isEmpty()) {
            throw new ConflictException("У пользователя уже есть необработанная заявка в команду");
        }
    }

    /**
     * Находит и валидирует членство капитана в конкретной команде.
     *
     * @param captainEmail email капитана
     * @param teamId идентификатор команды
     * @return активное членство капитана
     */
    private TeamMembership resolveCaptainMembership(String captainEmail, Long teamId) {
        User captain = getUserByEmail(captainEmail);

        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(teamId, captain.getId())
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        if (membership.getStatus() != TeamMembershipStatus.ACTIVE
                || membership.getRole() != TeamMembershipRole.CAPTAIN) {
            throw new BadRequestException("Только капитан команды может обрабатывать заявки");
        }

        return membership;
    }

    /**
     * Находит и валидирует активное капитанское членство пользователя.
     *
     * @param captainEmail email капитана
     * @return активное членство капитана
     */
    private TeamMembership resolveCaptainMembership(String captainEmail) {
        User captain = getUserByEmail(captainEmail);

        TeamMembership membership = teamMembershipRepository.findByUserIdAndStatus(captain.getId(), TeamMembershipStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("У пользователя нет команды"));

        if (membership.getRole() != TeamMembershipRole.CAPTAIN) {
            throw new BadRequestException("Только капитан команды может просматривать входящие заявки");
        }

        return membership;
    }

    /**
     * Возвращает заявку пользователя в статусе PENDING для указанной команды.
     *
     * @param teamId идентификатор команды
     * @param userId идентификатор пользователя
     * @return pending-членство
     */
    private TeamMembership getPendingMembership(Long teamId, Long userId) {
        TeamMembership membership = teamMembershipRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена"));

        if (membership.getStatus() != TeamMembershipStatus.PENDING) {
            throw new BadRequestException("Можно обработать только заявку в статусе PENDING");
        }

        return membership;
    }

    /**
     * Загружает пользователя по email.
     *
     * @param email email пользователя
     * @return найденный пользователь
     */
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    /**
     * Собирает DTO полной информации о команде.
     *
     * @param team команда
     * @return DTO команды
     */
    private TeamResponse buildTeamResponse(Team team) {
        List<TeamMemberResponse> members = teamMembershipRepository.findAllByTeamIdAndStatus(
                        team.getId(),
                        TeamMembershipStatus.ACTIVE
                ).stream()
                .map(this::buildTeamMemberResponse)
                .toList();

        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getCity(),
                team.getInviteCode(),
                team.getCaptain().getId(),
                team.getCaptain().getEmail(),
                team.getCreatedAt(),
                members
        );
    }

    /**
     * Собирает DTO участника команды.
     *
     * @param membership членство пользователя в команде
     * @return DTO участника
     */
    private TeamMemberResponse buildTeamMemberResponse(TeamMembership membership) {
        return new TeamMemberResponse(
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getRole(),
                membership.getStatus(),
                membership.getJoinedAt()
        );
    }

    /**
     * Собирает DTO результата обработки заявки на вступление.
     *
     * @param membership заявка на вступление
     * @return DTO результата обработки
     */
    private TeamJoinRequestDecisionResponse buildJoinRequestDecisionResponse(TeamMembership membership) {
        return new TeamJoinRequestDecisionResponse(
                membership.getTeam().getId(),
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getStatus(),
                membership.getJoinedAt(),
                membership.getUpdatedAt()
        );
    }

    /**
     * Собирает DTO входящей заявки для капитана команды.
     *
     * @param membership pending-заявка пользователя
     * @return DTO входящей заявки
     */
    private IncomingJoinRequestResponse buildIncomingJoinRequestResponse(TeamMembership membership) {
        return new IncomingJoinRequestResponse(
                membership.getTeam().getId(),
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getStatus(),
                membership.getCreatedAt()
        );
    }

    /**
     * Собирает DTO исходящей заявки пользователя в команду.
     *
     * @param membership pending-заявка пользователя
     * @return DTO исходящей заявки
     */
    private OutgoingJoinRequestResponse buildOutgoingJoinRequestResponse(TeamMembership membership) {
        return new OutgoingJoinRequestResponse(
                membership.getTeam().getId(),
                membership.getTeam().getName(),
                membership.getTeam().getCity(),
                membership.getStatus(),
                membership.getCreatedAt(),
                membership.getUpdatedAt()
        );
    }

    /**
     * Собирает компактное представление команды для списка.
     *
     * @param team команда
     * @return DTO элемента списка команд
     */
    private TeamListItemResponse buildTeamListItemResponse(Team team) {
        int activeMembersCount = teamMembershipRepository.findAllByTeamIdAndStatus(
                        team.getId(),
                        TeamMembershipStatus.ACTIVE
                ).size();

        return new TeamListItemResponse(
                team.getId(),
                team.getName(),
                team.getCity(),
                team.getCaptain().getId(),
                team.getCaptain().getEmail(),
                activeMembersCount,
                team.getCreatedAt()
        );
    }

    /**
     * Генерирует уникальный invite-код команды.
     *
     * @return уникальный invite-код
     */
    private String generateUniqueInviteCode() {
        SecureRandom random = new SecureRandom();
        String inviteCode;

        do {
            inviteCode = generateInviteCode(random);
        } while (teamRepository.existsByInviteCode(inviteCode));

        return inviteCode;
    }

    /**
     * Генерирует invite-код заданной длины из допустимого алфавита.
     *
     * @param random генератор случайных чисел
     * @return сгенерированный invite-код
     */
    private String generateInviteCode(SecureRandom random) {
        StringBuilder builder = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            int index = random.nextInt(INVITE_CODE_ALPHABET.length());
            builder.append(INVITE_CODE_ALPHABET.charAt(index));
        }
        return builder.toString();
    }

    /**
     * Нормализует invite-код перед поиском команды.
     *
     * @param inviteCode исходное значение invite-кода
     * @return invite-код в нормализованном виде
     */
    private String normalizeInviteCode(String inviteCode) {
        return inviteCode.trim().toUpperCase(Locale.ROOT);
    }
}
