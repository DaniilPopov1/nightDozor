package com.example.server.auth.repository;

import com.example.server.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий для работы с пользователями системы.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Ищет пользователя по email.
     *
     * @param email email пользователя
     * @return найденный пользователь, если существует
     */
    Optional<User> findByEmail(String email);

    /**
     * Проверяет, существует ли пользователь с указанным email.
     *
     * @param email email пользователя
     * @return {@code true}, если пользователь существует
     */
    boolean existsByEmail(String email);
}
