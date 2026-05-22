package com.keepcoding.dto;

import com.keepcoding.domain.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Payload recebido no POST /api/submissions.
 * O usuário NÃO vem no corpo — é obtido do token JWT autenticado.
 */
public record SubmissionRequest(

        @NotNull(message = "problemId e obrigatorio")
        Long problemId,

        @NotNull(message = "language e obrigatorio")
        Language language,

        @NotBlank(message = "code nao pode ser vazio")
        String code
) {}
