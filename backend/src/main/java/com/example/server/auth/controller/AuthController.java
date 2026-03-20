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
public class AuthController {

    private final RegistrationService registrationService;
    private final VerificationService verificationService;
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiMessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return ResponseEntity.ok(
                new ApiMessageResponse("Регистрация выполнена. Проверьте почту для подтверждения аккаунта.")
        );
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiMessageResponse> verify(@RequestParam String token) {
        verificationService.verify(token);
        return ResponseEntity.ok(
                new ApiMessageResponse("Почта подтверждена. Теперь можно войти в систему.")
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiMessageResponse> resend(@Valid @RequestBody ResendVerificationRequest request) {
        verificationService.resendVerification(request.email());
        return ResponseEntity.ok(
                new ApiMessageResponse("Письмо с подтверждением отправлено повторно.")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getUsername()));
    }
}
