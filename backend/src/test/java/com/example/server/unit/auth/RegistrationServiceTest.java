package com.example.server.unit.auth;

import com.example.server.auth.dto.RegisterRequest;
import com.example.server.auth.entity.Role;
import com.example.server.auth.entity.User;
import com.example.server.auth.entity.VerificationToken;
import com.example.server.auth.event.UserRegisteredEvent;
import com.example.server.auth.repository.UserRepository;
import com.example.server.auth.repository.VerificationTokenRepository;
import com.example.server.auth.service.RegistrationService;
import com.example.server.auth.service.VerificationTokenGenerator;
import com.example.server.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private VerificationTokenGenerator verificationTokenGenerator;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RegistrationService registrationService;

    // №1 — User(enabled=false) сохранён, VerificationToken сохранён, UserRegisteredEvent опубликован
    @Test
    void register_success_savesUserTokenAndPublishesEvent() {
        RegisterRequest req = new RegisterRequest("user@test.com", "pass123", Role.PARTICIPANT);
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("$hashed");
        when(verificationTokenGenerator.generate()).thenReturn("tok");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        registrationService.register(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("user@test.com", saved.getEmail());
        assertEquals("$hashed", saved.getPasswordHash());
        assertFalse(saved.isEnabled());

        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    // №2 — email нормализован: trim + toLowerCase, userRepository.save вызван с нормализованным email
    @Test
    void register_normalizesEmail() {
        RegisterRequest req = new RegisterRequest("  User@TEST.com  ", "pass123", Role.PARTICIPANT);
        when(userRepository.existsByEmail("user@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$h");
        when(verificationTokenGenerator.generate()).thenReturn("t");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        registrationService.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("user@test.com", captor.getValue().getEmail());
    }

    // №3 — token.expiresAt ∈ [now+24h−5s, now+24h+5s]
    @Test
    void register_verificationTokenExpiresIn24Hours() {
        RegisterRequest req = new RegisterRequest("u@test.com", "pass123", Role.PARTICIPANT);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$h");
        when(verificationTokenGenerator.generate()).thenReturn("t");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now().plus(24, ChronoUnit.HOURS).minusSeconds(5);
        registrationService.register(req);
        Instant after = Instant.now().plus(24, ChronoUnit.HOURS).plusSeconds(5);

        ArgumentCaptor<VerificationToken> captor = ArgumentCaptor.forClass(VerificationToken.class);
        verify(verificationTokenRepository).save(captor.capture());
        Instant expiresAt = captor.getValue().getExpiresAt();
        assertTrue(expiresAt.isAfter(before) && expiresAt.isBefore(after));
    }

    // №4 — дублирующийся email → ConflictException, userRepository.save не вызван
    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest req = new RegisterRequest("user@test.com", "pass123", Role.PARTICIPANT);
        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> registrationService.register(req));
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}