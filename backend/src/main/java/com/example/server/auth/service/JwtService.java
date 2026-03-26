package com.example.server.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
/**
 * Сервис генерации, парсинга и валидации JWT access token'ов.
 */
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-minutes}")
    private long accessTokenExpirationMinutes;

    private SecretKey signingKey;

    @PostConstruct
    /**
     * Инициализирует ключ подписи JWT из конфигурации приложения.
     */
    void init() {
        byte[] keyBytes = resolveSecretBytes(jwtSecret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Рассчитывает момент истечения срока действия access token.
     *
     * @return время истечения токена
     */
    public Instant calculateExpirationTime() {
        return Instant.now().plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);
    }

    /**
     * Генерирует JWT для пользователя.
     *
     * @param userDetails пользователь, для которого создается токен
     * @param expiresAt момент истечения токена
     * @return подписанный JWT
     */
    public String generateToken(UserDetails userDetails, Instant expiresAt) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(
                "role",
                userDetails.getAuthorities().stream()
                        .findFirst()
                        .map(Object::toString)
                        .orElse(null)
        );

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Извлекает email пользователя из токена.
     *
     * @param token JWT token
     * @return значение subject
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Проверяет, что токен принадлежит указанному пользователю и еще не истек.
     *
     * @param token JWT token
     * @param userDetails пользователь для проверки
     * @return {@code true}, если токен валиден
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        String username = claims.getSubject();

        return username.equals(userDetails.getUsername()) && claims.getExpiration().after(new Date());
    }

    /**
     * Извлекает claims из подписанного JWT.
     *
     * @param token JWT token
     * @return набор claims из токена
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Преобразует секрет из конфигурации в набор байтов.
     * Если строка не является Base64, используется ее UTF-8 представление.
     *
     * @param secret секрет из настроек приложения
     * @return байты секрета
     */
    private byte[] resolveSecretBytes(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (DecodingException | IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
