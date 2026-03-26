package com.example.server.auth.dto;

import com.example.server.auth.entity.Role;

import java.time.Instant;

/**
 * DTO профиля текущего аутентифицированного пользователя.
 *
 * @param id идентификатор пользователя
 * @param email email пользователя
 * @param role роль пользователя
 * @param enabled флаг подтвержденного аккаунта
 * @param accountNonLocked флаг незаблокированного аккаунта
 * @param createdAt дата создания пользователя
 */
public record CurrentUserResponse(
        Long id,
        String email,
        Role role,
        boolean enabled,
        boolean accountNonLocked,
        Instant createdAt
) {
}
