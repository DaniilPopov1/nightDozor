package com.example.server.common.exception;

/**
 * Исключение для ситуаций, когда запрос нарушает бизнес-правила или содержит некорректные данные.
 */
public class BadRequestException extends RuntimeException {

    /**
     * Создает исключение bad request с сообщением для клиента.
     *
     * @param message текст ошибки
     */
    public BadRequestException(String message) {
        super(message);
    }
}
