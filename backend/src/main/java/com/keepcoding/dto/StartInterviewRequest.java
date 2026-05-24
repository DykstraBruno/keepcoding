package com.keepcoding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload do POST /api/interviews/start. */
public record StartInterviewRequest(

        @NotBlank(message = "targetRole é obrigatório")
        @Size(max = 255)
        String targetRole,

        @NotBlank(message = "resumeText é obrigatório")
        @Size(min = 50, max = 20_000,
                message = "Currículo deve ter entre 50 e 20000 caracteres")
        String resumeText,

        /** Opcional; padrão 6, faixa permitida 3-12. */
        Integer maxQuestions
) {}
