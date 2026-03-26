package com.example.server.auth.service;

import com.example.server.auth.dto.AuthResponse;
import com.example.server.auth.dto.CurrentUserResponse;
import com.example.server.auth.dto.LoginRequest;
import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import com.example.server.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
/**
 * Сервис входа в систему и получения данных текущего пользователя.
 */
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Аутентифицирует пользователя по email и паролю и выпускает JWT access token.
     *
     * @param request данные для входа
     * @return ответ с токеном и данными пользователя
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(Locale.ROOT),
                        request.password()
                )
        );

        UserDetails user = (UserDetails) authentication.getPrincipal();
        Instant expiresAt = jwtService.calculateExpirationTime();
        String accessToken = jwtService.generateToken(user, expiresAt);

        return new AuthResponse(
                accessToken,
                "Bearer",
                expiresAt,
                user.getUsername(),
                extractRole(user)
        );
    }

    /**
     * Возвращает профиль пользователя по email из security context.
     *
     * @param email email текущего пользователя
     * @return DTO профиля пользователя
     */
    public CurrentUserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.isAccountNonLocked(),
                user.getCreatedAt()
        );
    }

    /**
     * Извлекает роль пользователя из списка authorities Spring Security.
     *
     * @param user пользователь из authentication principal
     * @return роль без префикса {@code ROLE_}
     */
    private String extractRole(UserDetails user) {
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("UNKNOWN");
    }
}
