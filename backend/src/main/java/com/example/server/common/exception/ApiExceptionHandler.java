package com.example.server.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
/**
 * Глобальный обработчик исключений REST API.
 * Преобразует бизнес-исключения и ошибки валидации в единый формат ответа.
 */
public class ApiExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    /**
     * Обрабатывает ошибки некорректного запроса.
     *
     * @param ex исключение bad request
     * @return HTTP-ответ со статусом 400
     */
    public ResponseEntity<?> handleBadRequest(BadRequestException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    /**
     * Обрабатывает конфликтные бизнес-сценарии.
     *
     * @param ex исключение конфликта
     * @return HTTP-ответ со статусом 409
     */
    public ResponseEntity<?> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    /**
     * Обрабатывает ошибки поиска сущностей.
     *
     * @param ex исключение отсутствия ресурса
     * @return HTTP-ответ со статусом 404
     */
    public ResponseEntity<?> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /**
     * Обрабатывает ошибки bean validation для входных DTO.
     *
     * @param ex исключение валидации аргументов контроллера
     * @return HTTP-ответ со статусом 400 и деталями по полям
     */
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", 400);
        body.put("error", "Validation error");
        body.put("fields", fields);

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Собирает типовой JSON-ответ для бизнес-исключений.
     *
     * @param status HTTP-статус ответа
     * @param message текст ошибки
     * @return сформированный HTTP-ответ
     */
    private ResponseEntity<?> build(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", message);

        return ResponseEntity.status(status).body(body);
    }
}
