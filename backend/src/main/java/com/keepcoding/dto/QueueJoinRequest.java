package com.keepcoding.dto;

import com.keepcoding.domain.enums.Difficulty;
import jakarta.validation.constraints.NotNull;

/** Payload do POST /api/matches/queue ou da mensagem STOMP /app/queue/join. */
public record QueueJoinRequest(
        @NotNull(message = "difficulty é obrigatório")
        Difficulty difficulty
) {}
