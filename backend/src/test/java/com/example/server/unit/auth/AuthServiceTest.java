package com.example.server.unit.auth;

import com.example.server.auth.dto.AuthResponse;
import com.example.server.auth.dto.CurrentUserResponse;
import com.example.server.auth.dto.LoginRequest;
import com.example.server.auth.entity.Role;
import com.example.server.auth.entity.User;
import com.example.server.auth.repository.UserRepository;
import com.example.server.auth.service.AuthService;
import com.example.server.auth.service.JwtService;
import com.example.server.common.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    // №1 — AuthResponse.tokenType = "Bearer", accessToken/expiresAt/email/role заполнены
    @Test
    void login_success_returnsAuthResponse() {
        UserDetails principal = new org.springframework.security.core.userdetails.User(
                "user@test.com", "$hash",
                List.of(new SimpleGrantedAuthority("ROLE_PARTICIPANT")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.calculateExpirationTime()).thenReturn(expiresAt);
        when(jwtService.generateToken(principal, expiresAt)).thenReturn("jwt.token.here");

        AuthResponse resp = authService.login(new LoginRequest("user@test.com", "pass123"));

        assertEquals("Bearer", resp.tokenType());
        assertEquals("jwt.token.here", resp.accessToken());
        assertEquals(expiresAt, resp.expiresAt());
        assertEquals("user@test.com", resp.email());
        assertEquals("PARTICIPANT", resp.role());
    }

    // №2 — неверный пароль → BadCredentialsException
    @Test
    void login_wrongPassword_throwsBadCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("user@test.com", "wrong")));
    }

    // №3 — аккаунт не активирован → DisabledException
    @Test
    void login_disabledAccount_throwsDisabledException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("Account disabled"));

        assertThrows(DisabledException.class,
                () -> authService.login(new LoginRequest("user@test.com", "pass123")));
    }

    // №1 getCurrentUser — пользователь найден, все поля совпадают
    @Test
    void getCurrentUser_userExists_returnsDto() {
        User user = new User();
        user.setId(42L);
        user.setEmail("user@test.com");
        user.setRole(Role.PARTICIPANT);
        user.setEnabled(true);
        user.setAccountNonLocked(true);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        CurrentUserResponse resp = authService.getCurrentUser("user@test.com");

        assertEquals(42L, resp.id());
        assertEquals("user@test.com", resp.email());
        assertEquals(Role.PARTICIPANT, resp.role());
        assertTrue(resp.enabled());
        assertTrue(resp.accountNonLocked());
    }

    // №2 getCurrentUser — пользователь не найден → NotFoundException
    @Test
    void getCurrentUser_userNotFound_throwsNotFoundException() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> authService.getCurrentUser("missing@test.com"));
        assertEquals("Пользователь не найден", ex.getMessage());
    }
}