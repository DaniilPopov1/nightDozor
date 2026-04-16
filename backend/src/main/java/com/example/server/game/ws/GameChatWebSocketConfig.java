package com.example.server.game.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
/**
 * Конфигурация websocket-эндпоинта игровых чатов.
 */
public class GameChatWebSocketConfig implements WebSocketConfigurer {

    private final GameChatWebSocketHandler gameChatWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameChatWebSocketHandler, "/ws/chat")
                .setAllowedOriginPatterns("*");
    }
}
