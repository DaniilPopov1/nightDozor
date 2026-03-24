package com.example.server.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitTaskKeyRequest(
        @NotBlank @Size(max = 255) String key
) {
}
