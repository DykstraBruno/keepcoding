package com.keepcoding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Saída estruturada do DevCoach na análise de arquitetura.
 * Espelha o JSON que a IA é instruída a devolver.
 */
public record ArchitectFeedback(

        /** Brilhante | Ótimo | Livro | Incompleto | Gafe */
        @JsonProperty("classification") String classification,

        /** Frase curta de impacto. */
        @JsonProperty("headline") String headline,

        /** Dica socrática: guia sem entregar a arquitetura pronta. */
        @JsonProperty("socratic_hint") String socraticHint,

        /** Avaliação curta da escalabilidade da proposta. */
        @JsonProperty("scalability") String scalability,

        /** Avaliação curta da resiliência / tolerância a falhas. */
        @JsonProperty("resilience") String resilience,

        /** Nota de qualidade de 0 a 100. */
        @JsonProperty("score") Integer score
) {}
