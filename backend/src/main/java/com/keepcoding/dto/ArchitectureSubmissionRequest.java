package com.keepcoding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload do POST /api/architecture/submissions.
 * O usuário vem do token JWT.
 */
public record ArchitectureSubmissionRequest(

        @NotNull(message = "challengeId é obrigatório")
        Long challengeId,

        @NotBlank(message = "mermaidCode não pode ser vazio")
        String mermaidCode,

        /** Justificativas das decisões de projeto (opcional). */
        String notes
) {}
