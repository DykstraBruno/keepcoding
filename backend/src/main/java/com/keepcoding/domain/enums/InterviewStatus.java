package com.keepcoding.domain.enums;

/** Estado de uma entrevista do KeepCoding. */
public enum InterviewStatus {
    /** Em andamento — ainda há perguntas a responder. */
    IN_PROGRESS,
    /** Encerrada normalmente; o feedback final já foi gerado. */
    COMPLETED,
    /** Encerrada antes do fim pelo usuário. */
    ABANDONED
}
