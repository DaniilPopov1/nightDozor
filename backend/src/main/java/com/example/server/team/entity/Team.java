package com.example.server.team.entity;

import com.example.server.auth.entity.User;
import com.example.server.game.entity.GameRegistration;
import com.example.server.game.entity.TeamGameRoute;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "teams",
        indexes = {
                @Index(name = "idx_teams_city", columnList = "city"),
                @Index(name = "idx_teams_captain_id", columnList = "captain_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(name = "invite_code", nullable = false, unique = true, length = 32)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "captain_id", nullable = false)
    private User captain;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "team")
    private Set<TeamMembership> memberships = new HashSet<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<GameRegistration> registrations = new HashSet<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<TeamGameRoute> routes = new HashSet<>();
}
