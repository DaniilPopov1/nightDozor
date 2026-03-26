package com.example.server.common.exception;

/**
 * Исключение для конфликтов состояния, когда операция не может быть выполнена в текущем контексте.
 */
public class ConflictException extends RuntimeException {

    /**
     * Создает исключение конфликта с сообщением для клиента.
     *
     * @param message текст ошибки
     */
    public ConflictException(String message) {
        super(message);
    }
}
