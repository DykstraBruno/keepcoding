package com.keepcoding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Feedback final estruturado da entrevista, gerado pela IA.
 * Espelha exatamente o JSON que o prompt instrui o modelo a devolver.
 */
public record InterviewFeedback(

        /** Excelente | Forte | Adequado | Limitado | Insuficiente */
        @JsonProperty("classification") String classification,

        /** Resumo curto (2-4 frases) da entrevista. */
        @JsonProperty("summary") String summary,

        /** Pontos fortes observados. */
        @JsonProperty("strengths") List<String> strengths,

        /** Lacunas / pontos a melhorar para a vaga. */
        @JsonProperty("gaps") List<String> gaps,

        /** Sugestões concretas de estudo / próximos passos. */
        @JsonProperty("suggestions") List<String> suggestions,

        /** Nota geral de 0 a 100. */
        @JsonProperty("score") Integer score
) {}
