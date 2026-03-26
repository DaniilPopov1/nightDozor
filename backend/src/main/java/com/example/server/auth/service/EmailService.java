package com.example.server.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
/**
 * Сервис отправки email-уведомлений, связанных с регистрацией и подтверждением аккаунта.
 */
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    /**
     * Отправляет письмо с ссылкой для подтверждения email.
     *
     * @param to email получателя
     * @param token токен подтверждения
     */
    public void sendVerificationEmail(String to, String token) {
        String verificationLink = frontendBaseUrl + "/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Подтверждение регистрации");
        message.setText("""
                Добро пожаловать!

                Для подтверждения почты перейдите по ссылке:
                %s

                Если это были не вы, просто проигнорируйте письмо.
                """.formatted(verificationLink));

        mailSender.send(message);
    }
}
