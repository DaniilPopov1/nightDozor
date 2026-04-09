package com.example.server.game.entity;

import com.example.server.team.entity.Team;
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
        name = "team_game_routes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_team_game_routes_game_slot", columnNames = {"game_id", "slot_number"})
        },
        indexes = {
                @Index(name = "idx_team_game_routes_game_id", columnList = "game_id"),
                @Index(name = "idx_team_game_routes_assigned_team_id", columnList = "assigned_team_id"),
                @Index(name = "idx_team_game_routes_slot_number", columnList = "slot_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
/**
 * Сущность маршрута слота в игре.
 * Определяет одну из заранее подготовленных цепочек заданий, которая позже может
 * быть случайно назначена подтверждённой команде.
 */
public class TeamGameRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "slot_number", nullable = false)
    private Integer slotNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id")
    private Team assignedTeam;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "route", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<TeamGameRouteItem> items = new HashSet<>();

    @OneToMany(mappedBy = "route", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<GameTeamSession> sessions = new HashSet<>();

    @PreUpdate
    /**
     * Обновляет время последнего изменения маршрута.
     */
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
