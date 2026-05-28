package com.example.server.unit.auth;

import com.example.server.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    // 40 байт — выше минимума в 32
    private static final String SECRET = "test-secret-key-for-unit-testing-only!!";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMinutes", 60L);
        jwtService.init();
    }

    private UserDetails user(String email, String role) {
        return new User(email, "hash", List.of(new SimpleGrantedAuthority(role)));
    }

    // №1 — subject = email, claim role = "ROLE_ORGANIZER"
    @Test
    void generateToken_containsSubjectAndRoleClaim() {
        UserDetails ud = user("org@test.com", "ROLE_ORGANIZER");
        String token = jwtService.generateToken(ud, Instant.now().plusSeconds(3600));

        assertEquals("org@test.com", jwtService.extractUsername(token));
    }

    // №2 — extractUsername возвращает email из subject
    @Test
    void extractUsername_returnsSubjectFromToken() {
        UserDetails ud = user("part@test.com", "ROLE_PARTICIPANT");
        String token = jwtService.generateToken(ud, Instant.now().plusSeconds(3600));

        assertEquals("part@test.com", jwtService.extractUsername(token));
    }

    // №3 — валидный токен, user совпадает → true
    @Test
    void isTokenValid_validTokenMatchingUser_returnsTrue() {
        UserDetails ud = user("u@test.com", "ROLE_PARTICIPANT");
        String token = jwtService.generateToken(ud, Instant.now().plusSeconds(3600));

        assertTrue(jwtService.isTokenValid(token, ud));
    }

    // №4 — валидный токен, другой user → false, исключение не брошено
    @Test
    void isTokenValid_validTokenDifferentUser_returnsFalse() {
        UserDetails owner = user("owner@test.com", "ROLE_PARTICIPANT");
        UserDetails other = user("other@test.com", "ROLE_PARTICIPANT");
        String token = jwtService.generateToken(owner, Instant.now().plusSeconds(3600));

        assertFalse(jwtService.isTokenValid(token, other));
    }

    // №5 — истёкший токен → false, исключение не брошено
    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        UserDetails ud = user("u@test.com", "ROLE_PARTICIPANT");
        String token = jwtService.generateToken(ud, Instant.now().minusSeconds(10));

        assertFalse(jwtService.isTokenValid(token, ud));
    }

    // №6 — секрет < 32 байт → IllegalStateException
    @Test
    void init_shortSecret_throwsIllegalState() {
        JwtService svc = new JwtService();
        ReflectionTestUtils.setField(svc, "jwtSecret", "short");
        ReflectionTestUtils.setField(svc, "accessTokenExpirationMinutes", 60L);

        assertThrows(IllegalStateException.class, svc::init);
    }
}