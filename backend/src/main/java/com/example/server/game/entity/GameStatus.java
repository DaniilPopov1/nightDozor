package com.example.server.game.entity;

/**
 * Жизненный цикл игры.
 */
public enum GameStatus {
    DRAFT,
    REGISTRATION_OPEN,
    REGISTRATION_CLOSED,
    IN_PROGRESS,
    FINISHED,
    CANCELED
}
