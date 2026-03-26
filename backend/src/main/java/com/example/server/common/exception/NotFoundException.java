package com.example.server.common.exception;

/**
 * Исключение для случаев, когда запрошенный ресурс не найден.
 */
public class NotFoundException extends RuntimeException {

    /**
     * Создает исключение отсутствующего ресурса с сообщением для клиента.
     *
     * @param message текст ошибки
     */
    public NotFoundException(String message) {
        super(message);
    }
}
