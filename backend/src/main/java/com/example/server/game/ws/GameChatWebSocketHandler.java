package com.example.server.game.ws;

import com.example.server.auth.service.JwtService;
import com.example.server.game.dto.GameChatMessageResponse;
import com.example.server.game.entity.GameChatChannel;
import com.example.server.game.service.GameService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
/**
 * WebSocket-хендлер для игровых чатов.
 * Формат входящих событий:
 * - SUBSCRIBE: {type, gameId, teamId, channel}
 * - UNSUBSCRIBE: {type, gameId, teamId, channel}
 * - SEND: {type, gameId, teamId, channel, text}
 */
public class GameChatWebSocketHandler extends TextWebSocketHandler {

    private static final String TYPE_SUBSCRIBE = "SUBSCRIBE";
    private static final String TYPE_UNSUBSCRIBE = "UNSUBSCRIBE";
    private static final String TYPE_SEND = "SEND";

    private final JwtService jwtService;
    private final GameService gameService;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, String> emailBySessionId = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> keysBySessionId = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionsBySubscriptionKey = new ConcurrentHashMap<>();

    public GameChatWebSocketHandler(JwtService jwtService, GameService gameService) {
        this.jwtService = jwtService;
        this.gameService = gameService;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        if (token == null || token.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("JWT token is required"));
            return;
        }

        String email;
        try {
            email = jwtService.extractUsername(token);
        } catch (RuntimeException ex) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid JWT token"));
            return;
        }

        sessionsById.put(session.getId(), session);
        emailBySessionId.put(session.getId(), email);
        keysBySessionId.put(session.getId(), ConcurrentHashMap.newKeySet());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String email = emailBySessionId.get(session.getId());
        if (email == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized websocket session"));
            return;
        }

        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = readRequiredText(payload, "type");
            Long gameId = readRequiredLong(payload, "gameId");
            Long teamId = readRequiredLong(payload, "teamId");
            GameChatChannel channel = GameChatChannel.valueOf(readRequiredText(payload, "channel"));
            String subscriptionKey = buildSubscriptionKey(gameId, teamId, channel);

            switch (type) {
                case TYPE_SUBSCRIBE -> handleSubscribe(session, email, gameId, teamId, channel, subscriptionKey);
                case TYPE_UNSUBSCRIBE -> handleUnsubscribe(session, subscriptionKey);
                case TYPE_SEND -> handleSend(session, email, payload, gameId, teamId, channel, subscriptionKey);
                default -> sendError(session, "Unsupported websocket event type: " + type);
            }
        } catch (Exception ex) {
            sendError(session, ex.getMessage() != null ? ex.getMessage() : "Chat websocket error");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanupSession(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        cleanupSession(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void handleSubscribe(
            WebSocketSession session,
            String email,
            Long gameId,
            Long teamId,
            GameChatChannel channel,
            String subscriptionKey
    ) throws IOException {
        gameService.validateChatAccess(email, gameId, teamId, channel);

        keysBySessionId.computeIfAbsent(session.getId(), ignored -> ConcurrentHashMap.newKeySet())
                .add(subscriptionKey);
        sessionsBySubscriptionKey.computeIfAbsent(subscriptionKey, ignored -> ConcurrentHashMap.newKeySet())
                .add(session.getId());

        sendEvent(session, "SUBSCRIBED", Map.of(
                "gameId", gameId,
                "teamId", teamId,
                "channel", channel.name()
        ));
    }

    private void handleUnsubscribe(WebSocketSession session, String subscriptionKey) throws IOException {
        removeSubscription(session.getId(), subscriptionKey);
        sendEvent(session, "UNSUBSCRIBED", Map.of("key", subscriptionKey));
    }

    private void handleSend(
            WebSocketSession session,
            String email,
            JsonNode payload,
            Long gameId,
            Long teamId,
            GameChatChannel channel,
            String subscriptionKey
    ) throws IOException {
        String text = readRequiredText(payload, "text");
        GameChatMessageResponse savedMessage = gameService.sendChatMessage(email, gameId, teamId, channel, text);

        broadcast(subscriptionKey, "MESSAGE", Map.of(
                "gameId", gameId,
                "teamId", teamId,
                "channel", channel.name(),
                "message", savedMessage
        ));
    }

    private void broadcast(String subscriptionKey, String type, Map<String, Object> payload) throws IOException {
        Set<String> sessionIds = sessionsBySubscriptionKey.getOrDefault(subscriptionKey, Set.of());
        if (sessionIds.isEmpty()) {
            return;
        }

        for (String sessionId : sessionIds) {
            WebSocketSession targetSession = sessionsById.get(sessionId);
            if (targetSession == null || !targetSession.isOpen()) {
                continue;
            }
            sendEvent(targetSession, type, payload);
        }
    }

    private void cleanupSession(String sessionId) {
        Set<String> keys = keysBySessionId.remove(sessionId);
        if (keys != null) {
            for (String key : keys) {
                removeSubscription(sessionId, key);
            }
        }

        sessionsById.remove(sessionId);
        emailBySessionId.remove(sessionId);
    }

    private void removeSubscription(String sessionId, String key) {
        Set<String> keys = keysBySessionId.get(sessionId);
        if (keys != null) {
            keys.remove(key);
        }

        Set<String> sessionIds = sessionsBySubscriptionKey.get(key);
        if (sessionIds == null) {
            return;
        }

        sessionIds.remove(sessionId);
        if (sessionIds.isEmpty()) {
            sessionsBySubscriptionKey.remove(key);
        }
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        sendEvent(session, "ERROR", Map.of("error", message));
    }

    private void sendEvent(WebSocketSession session, String type, Map<String, Object> payload) throws IOException {
        String json = objectMapper.writeValueAsString(Map.of(
                "type", type,
                "payload", payload
        ));
        session.sendMessage(new TextMessage(json));
    }

    private String buildSubscriptionKey(Long gameId, Long teamId, GameChatChannel channel) {
        return gameId + ":" + teamId + ":" + channel.name();
    }

    private String readRequiredText(JsonNode payload, String fieldName) {
        JsonNode node = payload.get(fieldName);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required websocket field: " + fieldName);
        }
        return node.asText();
    }

    private Long readRequiredLong(JsonNode payload, String fieldName) {
        JsonNode node = payload.get(fieldName);
        if (node == null || !node.canConvertToLong()) {
            throw new IllegalArgumentException("Missing required websocket field: " + fieldName);
        }
        return node.asLong();
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null || query.isBlank()) {
            return null;
        }

        for (String queryParam : query.split("&")) {
            String[] parts = queryParam.split("=", 2);
            if (parts.length == 2 && "token".equals(parts[0])) {
                return parts[1];
            }
        }

        return null;
    }
}
