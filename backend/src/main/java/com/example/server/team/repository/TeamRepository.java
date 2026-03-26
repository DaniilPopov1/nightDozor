package com.example.server.team.repository;

import com.example.server.team.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с командами.
 */
public interface TeamRepository extends JpaRepository<Team, Long> {
    /**
     * Возвращает все команды в порядке убывания даты создания.
     *
     * @return список команд
     */
    List<Team> findAllByOrderByCreatedAtDesc();

    /**
     * Возвращает команды указанного города в порядке убывания даты создания.
     *
     * @param city город команды
     * @return список команд
     */
    List<Team> findAllByCityIgnoreCaseOrderByCreatedAtDesc(String city);

    /**
     * Ищет команду по invite-коду.
     *
     * @param inviteCode код приглашения
     * @return найденная команда
     */
    Optional<Team> findByInviteCode(String inviteCode);

    /**
     * Проверяет существование команды с указанным invite-кодом.
     *
     * @param inviteCode код приглашения
     * @return {@code true}, если команда существует
     */
    boolean existsByInviteCode(String inviteCode);
}
