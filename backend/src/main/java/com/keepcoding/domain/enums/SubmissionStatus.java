package com.keepcoding.domain.enums;

/** Veredito da execucao de uma submissao no sandbox. */
public enum SubmissionStatus {
    /** Ainda nao processada. */
    PENDING,
    /** Passou em todos os casos de teste. */
    ACCEPTED,
    /** Saida diferente da esperada. */
    WRONG_ANSWER,
    /** Estourou o tempo limite. */
    TIME_LIMIT,
    /** Erro de compilacao ou runtime. */
    ERROR
}
