package com.example.server.game.dto;

import java.time.Instant;
import java.util.List;

public record TeamGameRouteResponse(
        Long id,
        Long gameId,
        Long teamId,
        String teamName,
        String name,
        Instant createdAt,
        List<TeamGameRouteItemResponse> items
) {
}
