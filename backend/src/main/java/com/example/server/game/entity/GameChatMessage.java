package com.example.server.game.entity;

import com.example.server.auth.entity.User;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "game_chat_messages",
        indexes = {
                @Index(name = "idx_game_chat_messages_game_id", columnList = "game_id"),
                @Index(name = "idx_game_chat_messages_team_id", columnList = "team_id"),
                @Index(name = "idx_game_chat_messages_channel", columnList = "channel"),
                @Index(name = "idx_game_chat_messages_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
/**
 * Сообщение игрового чата в контексте пары игра-команда.
 */
public class GameChatMessage {

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
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GameChatChannel channel;

    @Column(nullable = false, length = 4000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
