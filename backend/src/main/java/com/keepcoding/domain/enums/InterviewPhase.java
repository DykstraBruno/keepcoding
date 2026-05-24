package com.keepcoding.domain.enums;

/**
 * Fase atual de uma entrevista no formato padrão (40 min):
 * 10 min de apresentação + 30 min de perguntas (~4 min por pergunta).
 */
public enum InterviewPhase {
    /** Primeiro turno: candidato se apresenta (até 10 min). */
    PRESENTATION,
    /** Bloco de perguntas técnicas/comportamentais (~4 min cada). */
    QUESTIONS,
    /** Entrevista encerrada — feedback final disponível. */
    COMPLETED
}
