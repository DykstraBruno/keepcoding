package com.keepcoding.dto;

import com.keepcoding.domain.enums.Difficulty;

/** Status entregue ao próprio usuário enquanto está na fila de matchmaking. */
public record QueueStatusDto(
        String state,           // "WAITING" | "LEFT" | "ERROR"
        Difficulty difficulty,
        String message
) {}
