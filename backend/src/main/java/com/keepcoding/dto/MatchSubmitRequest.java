package com.keepcoding.dto;

import com.keepcoding.domain.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Payload do POST /api/matches/{id}/submit. */
public record MatchSubmitRequest(
        @NotNull(message = "language é obrigatório")
        Language language,

        @NotBlank(message = "code não pode ser vazio")
        String code
) {}
