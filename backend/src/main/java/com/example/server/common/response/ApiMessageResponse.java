package com.example.server.common.response;

/**
 * Универсальный DTO-ответ с текстовым сообщением.
 *
 * @param message сообщение для клиента
 */
public record ApiMessageResponse(String message) {
}
