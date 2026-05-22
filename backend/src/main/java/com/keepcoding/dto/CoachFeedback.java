package com.keepcoding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Saida estruturada do DevCoach.
 * Espelha EXATAMENTE o JSON que a IA e instruida a devolver no system prompt.
 * O Spring AI converte a resposta do modelo diretamente para este record.
 */
public record CoachFeedback(

        /** Brilhante | Otimo | Livro | Incompleto | Gafe */
        @JsonProperty("classification") String classification,

        /** Frase curta de impacto, estilo Chess.com. */
        @JsonProperty("headline") String headline,

        /** Dica socratica: guia sem revelar a resposta. */
        @JsonProperty("socratic_hint") String socraticHint,

        /** Complexidade de tempo, ex.: O(n). */
        @JsonProperty("time_complexity") String timeComplexity,

        /** Complexidade de espaco, ex.: O(1). */
        @JsonProperty("space_complexity") String spaceComplexity,

        /** Nota de qualidade de 0 a 100. */
        @JsonProperty("score") Integer score
) {}
