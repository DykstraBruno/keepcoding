package com.keepcoding.dto;

import com.keepcoding.domain.enums.SubmissionStatus;

import java.time.Instant;

/** Resposta devolvida após processar uma submissão. */
public record SubmissionResponse(
        Long submissionId,
        SubmissionStatus status,
        Integer passedTests,
        Integer totalTests,
        CoachFeedback coachFeedback,
        Instant createdAt,
        /** XP creditado nesta submissão (0 se não houve primeiro acerto). */
        Integer xpAwarded,
        /** true se esta foi a primeira vez que o usuário resolveu o problema. */
        Boolean firstSolve,
        /** XP total do usuário após esta submissão. */
        Integer userXp
) {}
