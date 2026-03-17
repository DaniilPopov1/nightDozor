package com.example.server.auth.service;

import com.example.server.auth.entity.User;
import com.example.server.auth.entity.VerificationToken;
import com.example.server.auth.repository.UserRepository;
import com.example.server.auth.repository.VerificationTokenRepository;
import com.example.server.common.exception.BadRequestException;
import com.example.server.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final VerificationTokenGenerator verificationTokenGenerator;
    private final EmailService emailService;

    @Transactional
    public void verify(String rawToken) {
        VerificationToken token = verificationTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new BadRequestException("Неверный токен подтверждения"));

        if (token.isUsed()) {
            throw new BadRequestException("Токен уже использован");
        }

        if (token.isExpired()) {
            throw new BadRequestException("Срок действия токена истек");
        }

        User user = token.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        verificationTokenRepository.save(token);
    }

    @Transactional
    public void resendVerification(String emailRaw) {
        String email = emailRaw.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (user.isEnabled()) {
            throw new BadRequestException("Почта уже подтверждена");
        }

        verificationTokenRepository.deleteAllByUser(user);

        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setToken(verificationTokenGenerator.generate());
        token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        verificationTokenRepository.save(token);

        emailService.sendVerificationEmail(user.getEmail(), token.getToken());
    }
}
