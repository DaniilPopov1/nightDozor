package com.example.server.game.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "game_task_hints",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_game_task_hints_task_order", columnNames = {"task_id", "order_index"})
        },
        indexes = {
                @Index(name = "idx_game_task_hints_task_id", columnList = "task_id"),
                @Index(name = "idx_game_task_hints_order_index", columnList = "order_index")
        }
)
@Getter
@Setter
@NoArgsConstructor
/**
 * Сущность подсказки задания.
 * Хранит текст подсказки и задержку ее открытия.
 */
public class GameTaskHint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private GameTask task;

    @Column(nullable = false, length = 4000)
    private String text;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "delay_minutes_from_previous_hint", nullable = false)
    private Integer delayMinutesFromPreviousHint;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    /**
     * Обновляет время последнего изменения подсказки.
     */
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
