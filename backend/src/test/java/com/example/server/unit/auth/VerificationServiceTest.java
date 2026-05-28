package com.example.server.unit.auth;

import com.example.server.auth.entity.User;
import com.example.server.auth.entity.VerificationToken;
import com.example.server.auth.repository.UserRepository;
import com.example.server.auth.repository.VerificationTokenRepository;
import com.example.server.auth.service.EmailService;
import com.example.server.auth.service.VerificationService;
import com.example.server.auth.service.VerificationTokenGenerator;
import com.example.server.common.exception.BadRequestException;
import com.example.server.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private VerificationTokenGenerator tokenGenerator;
    @Mock private EmailService emailService;

    @InjectMocks
    private VerificationService verificationService;

    // ── verify ────────────────────────────────────────────────────────────────

    // №1 — валидный токен → user.enabled=true, token.usedAt≠null
    @Test
    void verify_validToken_enablesUserAndMarksTokenUsed() {
        User user = new User();
        user.setEnabled(false);

        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS)); // не истёк
        // usedAt=null → isUsed()=false

        when(tokenRepository.findByToken("valid-tok")).thenReturn(Optional.of(token));
        when(userRepository.save(user)).thenReturn(user);
        when(tokenRepository.save(token)).thenReturn(token);

        verificationService.verify("valid-tok");

        assertTrue(user.isEnabled());
        assertNotNull(token.getUsedAt());
    }

    // №2 — токен не найден → BadRequestException("Неверный токен подтверждения")
    @Test
    void verify_tokenNotFound_throwsBadRequest() {
        when(tokenRepository.findByToken("bad")).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> verificationService.verify("bad"));
        assertEquals("Неверный токен подтверждения", ex.getMessage());
    }

    // №3 — токен уже использован → BadRequestException, user.enabled не изменён
    @Test
    void verify_alreadyUsedToken_throwsBadRequest() {
        User user = new User();
        user.setEnabled(false);

        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        token.setUsedAt(Instant.now().minusSeconds(60)); // isUsed() = true

        when(tokenRepository.findByToken("used-tok")).thenReturn(Optional.of(token));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> verificationService.verify("used-tok"));
        assertEquals("Токен уже использован", ex.getMessage());
        assertFalse(user.isEnabled());
    }

    // №4 — токен истёк → BadRequestException, user.enabled не изменён
    @Test
    void verify_expiredToken_throwsBadRequest() {
        User user = new User();
        user.setEnabled(false);

        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setExpiresAt(Instant.now().minusSeconds(60)); // isExpired() = true
        // usedAt=null → isUsed()=false

        when(tokenRepository.findByToken("expired-tok")).thenReturn(Optional.of(token));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> verificationService.verify("expired-tok"));
        assertEquals("Срок действия токена истек", ex.getMessage());
        assertFalse(user.isEnabled());
    }

    // ── resendVerification ────────────────────────────────────────────────────

    // №1 — успех: старые токены удалены, новый сохранён, emailService вызван
    @Test
    void resendVerification_success_deletesOldSavesNewSendsEmail() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setEnabled(false);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(tokenGenerator.generate()).thenReturn("new-tok");
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        verificationService.resendVerification("user@test.com");

        verify(tokenRepository).deleteAllByUser(user);
        ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(tokenRepository).save(captor.capture());
        assertEquals("new-tok", captor.getValue().getToken());
        verify(emailService).sendVerificationEmail("user@test.com", "new-tok");
    }

    // №2 — email нормализован до обращения в БД
    @Test
    void resendVerification_normalizesEmail() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setEnabled(false);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(tokenGenerator.generate()).thenReturn("t");
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        verificationService.resendVerification("  USER@TEST.COM  ");

        verify(userRepository).findByEmail("user@test.com");
    }

    // №3 — пользователь не найден → NotFoundException
    @Test
    void resendVerification_userNotFound_throwsNotFoundException() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> verificationService.resendVerification("missing@test.com"));
        assertEquals("Пользователь не найден", ex.getMessage());
    }

    // №4 — email уже подтверждён → BadRequestException, deleteAllByUser не вызван
    @Test
    void resendVerification_alreadyEnabled_throwsBadRequest() {
        User user = new User();
        user.setEmail("user@test.com");
        user.setEnabled(true);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> verificationService.resendVerification("user@test.com"));
        assertEquals("Почта уже подтверждена", ex.getMessage());
        verify(tokenRepository, never()).deleteAllByUser(any());
    }
}