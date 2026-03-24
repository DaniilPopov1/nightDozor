package com.example.server.game.entity;

import com.example.server.auth.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "games",
        indexes = {
                @Index(name = "idx_games_city", columnList = "city"),
                @Index(name = "idx_games_status", columnList = "status"),
                @Index(name = "idx_games_organizer_id", columnList = "organizer_id"),
                @Index(name = "idx_games_starts_at", columnList = "starts_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 120)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameStatus status = GameStatus.DRAFT;

    @Column(name = "min_team_size", nullable = false)
    private Integer minTeamSize;

    @Column(name = "max_team_size", nullable = false)
    private Integer maxTeamSize;

    @Column(name = "task_failure_penalty_minutes", nullable = false)
    private Integer taskFailurePenaltyMinutes;

    @Column(name = "registration_starts_at")
    private Instant registrationStartsAt;

    @Column(name = "registration_ends_at")
    private Instant registrationEndsAt;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @OneToMany(mappedBy = "game", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<GameRegistration> registrations = new HashSet<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<GameTask> tasks = new HashSet<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<TeamGameRoute> routes = new HashSet<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<GameTeamSession> sessions = new HashSet<>();

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
