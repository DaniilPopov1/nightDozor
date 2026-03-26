package com.example.server.auth.repository;

import com.example.server.auth.entity.User;
import com.example.server.auth.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для работы с токенами подтверждения email.
 */
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    /**
     * Ищет токен подтверждения по его строковому значению.
     *
     * @param token строковое значение токена
     * @return найденный токен, если существует
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * Удаляет все токены подтверждения, связанные с пользователем.
     *
     * @param user пользователь, для которого удаляются токены
     */
    void deleteAllByUser(User user);
}
