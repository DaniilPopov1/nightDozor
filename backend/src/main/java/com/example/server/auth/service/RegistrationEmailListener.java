package com.example.server.auth.service;

import com.example.server.auth.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * Слушатель события успешной регистрации, отправляющий письмо после коммита транзакции.
 */
public class RegistrationEmailListener {

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    /**
     * Обрабатывает событие регистрации и инициирует отправку письма подтверждения.
     *
     * @param event событие регистрации пользователя
     */
    public void handle(UserRegisteredEvent event) {
        try {
            emailService.sendVerificationEmail(event.email(), event.token());
        } catch (Exception ex) {
            log.error("Не удалось отправить письмо подтверждения на {}: {}", event.email(), ex.getMessage(), ex);
        }
    }
}
