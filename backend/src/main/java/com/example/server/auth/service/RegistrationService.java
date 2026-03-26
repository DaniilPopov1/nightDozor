package com.example.server.auth.service;

import com.example.server.auth.dto.RegisterRequest;
import com.example.server.auth.entity.User;
import com.example.server.auth.entity.VerificationToken;
import com.example.server.auth.event.UserRegisteredEvent;
import com.example.server.auth.repository.UserRepository;
import com.example.server.auth.repository.VerificationTokenRepository;
import com.example.server.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
@RequiredArgsConstructor
/**
 * Сервис регистрации нового пользователя и выпуска токена подтверждения email.
 */
public class RegistrationService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenGenerator verificationTokenGenerator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    /**
     * Создает нового пользователя, сохраняет токен подтверждения и публикует событие регистрации.
     *
     * @param request данные пользователя для регистрации
     */
    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Пользователь с таким email уже существует");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setEnabled(false);

        userRepository.save(user);

        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setToken(verificationTokenGenerator.generate());
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        verificationTokenRepository.save(token);

        eventPublisher.publishEvent(
                new UserRegisteredEvent(user.getId(), user.getEmail(), token.getToken())
        );
    }

    /**
     * Нормализует email перед сохранением и поиском.
     *
     * @param email исходное значение email
     * @return email в нормализованном виде
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
