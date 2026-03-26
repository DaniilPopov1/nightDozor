package com.example.server.auth.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
/**
 * Генератор криптографически стойких токенов для подтверждения email.
 */
public class VerificationTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Генерирует URL-safe токен без padding.
     *
     * @return строковое представление токена
     */
    public String generate() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
