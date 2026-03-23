package com.example.server.game.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "game_tasks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_game_tasks_game_order", columnNames = {"game_id", "order_index"})
        },
        indexes = {
                @Index(name = "idx_game_tasks_game_id", columnList = "game_id"),
                @Index(name = "idx_game_tasks_order_index", columnList = "order_index")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class GameTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "riddle_text", nullable = false, length = 4000)
    private String riddleText;

    @Column(name = "answer_key", nullable = false, length = 255)
    private String answerKey;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "time_limit_minutes", nullable = false)
    private Integer timeLimitMinutes;

    @Column(name = "failure_penalty_minutes", nullable = false)
    private Integer failurePenaltyMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "task", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<GameTaskHint> hints = new HashSet<>();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
