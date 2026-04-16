package com.example.server.auth.security;

import com.example.server.auth.service.JwtAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
/**
 * Конфигурация Spring Security для JWT-аутентификации и защиты API.
 */
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    /**
     * Настраивает цепочку фильтров безопасности, публичные auth-endpoint'ы и stateless-сессии.
     *
     * @param http объект конфигурации HTTP-безопасности
     * @return настроенная цепочка фильтров
     * @throws Exception если при сборке конфигурации возникает ошибка
     */
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/verify",
                                "/api/auth/resend-verification",
                                "/api/auth/login",
                                "/ws/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

                            String message = resolveAuthMessage(authException);

                            new ObjectMapper().writeValue(
                                    response.getWriter(),
                                    Map.of("error", message)
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

                            new ObjectMapper().writeValue(
                                    response.getWriter(),
                                    Map.of("error", "Доступ запрещен")
                            );
                        })
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Преобразует исключение аутентификации в понятное сообщение для API-ответа.
     *
     * @param ex исключение аутентификации
     * @return текст ошибки для клиента
     */
    private String resolveAuthMessage(AuthenticationException ex) {
        if (ex instanceof DisabledException) {
            return "Подтвердите email перед входом";
        }
        if (ex instanceof LockedException) {
            return "Аккаунт заблокирован";
        }
        if (ex instanceof BadCredentialsException) {
            return "Неверный email или пароль";
        }
        return "Ошибка аутентификации";
    }

    @Bean
    /**
     * Создает encoder для безопасного хеширования паролей.
     *
     * @return экземпляр BCrypt password encoder
     */
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    /**
     * Предоставляет {@link AuthenticationManager}, собранный Spring Security.
     *
     * @param configuration объект конфигурации аутентификации
     * @return authentication manager
     * @throws Exception если manager не может быть создан
     */
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
