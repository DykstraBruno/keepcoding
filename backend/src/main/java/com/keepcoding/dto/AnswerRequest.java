package com.keepcoding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload do POST /api/interviews/{id}/answer. */
public record AnswerRequest(

        @NotBlank(message = "Resposta não pode ser vazia")
        @Size(max = 10_000, message = "Resposta muito longa (máx. 10000 caracteres)")
        String content
) {}
