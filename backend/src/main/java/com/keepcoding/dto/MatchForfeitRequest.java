package com.keepcoding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload do POST /api/matches/{id}/forfeit.
 * Usado pelo frontend quando detecta uma violação de anti-cheat
 * (troca de aba, perda de foco, saída de fullscreen) — ou quando o
 * usuário desiste voluntariamente.
 */
public record MatchForfeitRequest(
        @NotBlank
        @Size(max = 64)
        String reason
) {}
