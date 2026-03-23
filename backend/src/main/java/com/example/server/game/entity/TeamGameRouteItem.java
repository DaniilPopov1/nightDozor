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
        name = "team_game_route_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_team_game_route_items_route_order", columnNames = {"route_id", "order_index"}),
                @UniqueConstraint(name = "uk_team_game_route_items_route_task", columnNames = {"route_id", "task_id"})
        },
        indexes = {
                @Index(name = "idx_team_game_route_items_route_id", columnList = "route_id"),
                @Index(name = "idx_team_game_route_items_task_id", columnList = "task_id"),
                @Index(name = "idx_team_game_route_items_order_index", columnList = "order_index")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class TeamGameRouteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private TeamGameRoute route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private GameTask task;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
