package com.example.server.game.entity;

import com.example.server.team.entity.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "game_team_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_game_team_sessions_game_team", columnNames = {"game_id", "team_id"})
        },
        indexes = {
                @Index(name = "idx_game_team_sessions_game_id", columnList = "game_id"),
                @Index(name = "idx_game_team_sessions_team_id", columnList = "team_id"),
                @Index(name = "idx_game_team_sessions_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class GameTeamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private TeamGameRoute route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_route_item_id", nullable = false)
    private TeamGameRouteItem currentRouteItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "current_task_id", nullable = false)
    private GameTask currentTask;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameTeamSessionStatus status;

    @Column(name = "current_order_index", nullable = false)
    private Integer currentOrderIndex;

    @Column(name = "total_penalty_minutes", nullable = false)
    private Integer totalPenaltyMinutes = 0;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "current_task_started_at", nullable = false)
    private Instant currentTaskStartedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
