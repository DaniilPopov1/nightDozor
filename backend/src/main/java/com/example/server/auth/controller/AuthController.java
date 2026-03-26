package com.example.server.auth.controller;

import com.example.server.auth.dto.AuthResponse;
import com.example.server.auth.dto.CurrentUserResponse;
import com.example.server.auth.dto.LoginRequest;
import com.example.server.auth.dto.RegisterRequest;
import com.example.server.auth.dto.ResendVerificationRequest;
import com.example.server.auth.service.AuthService;
import com.example.server.auth.service.RegistrationService;
import com.example.server.auth.service.VerificationService;
import com.example.server.common.response.ApiMessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
/**
 * REST-контроллер для регистрации, подтверждения почты и аутентификации пользователей.
 */
public class AuthController {

    private final RegistrationService registrationService;
    private final VerificationService verificationService;
    private final AuthService authService;

    /**
     * Регистрирует нового пользователя и инициирует отправку письма с подтверждением.
     *
     * @param request данные для регистрации
     * @return сообщение об успешном создании регистрации
     */
    @PostMapping("/register")
    public ResponseEntity<ApiMessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return ResponseEntity.ok(
                new ApiMessageResponse("Регистрация выполнена. Проверьте почту для подтверждения аккаунта.")
        );
    }

    /**
     * Подтверждает email пользователя по токену из письма.
     *
     * @param token токен подтверждения
     * @return сообщение об успешном подтверждении почты
     */
    @GetMapping("/verify")
    public ResponseEntity<ApiMessageResponse> verify(@RequestParam String token) {
        verificationService.verify(token);
        return ResponseEntity.ok(
                new ApiMessageResponse("Почта подтверждена. Теперь можно войти в систему.")
        );
    }

    /**
     * Повторно отправляет письмо с подтверждением для неподтвержденного аккаунта.
     *
     * @param request email пользователя
     * @return сообщение об отправке письма
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiMessageResponse> resend(@Valid @RequestBody ResendVerificationRequest request) {
        verificationService.resendVerification(request.email());
        return ResponseEntity.ok(
                new ApiMessageResponse("Письмо с подтверждением отправлено повторно.")
        );
    }

    /**
     * Выполняет аутентификацию пользователя и возвращает JWT access token.
     *
     * @param request данные для входа
     * @return токен и базовая информация о пользователе
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Возвращает профиль текущего аутентифицированного пользователя.
     *
     * @param userDetails пользователь из security context
     * @return данные текущего пользователя
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getUsername()));
    }
}
