package com.example.server.auth.service;

import com.example.server.auth.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
        emailService.sendVerificationEmail(event.email(), event.token());
    }
}
