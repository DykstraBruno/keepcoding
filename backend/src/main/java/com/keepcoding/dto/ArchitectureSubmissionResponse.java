package com.keepcoding.dto;

import java.time.Instant;

/** Resposta de uma submissão de arquitetura: feedback do DevCoach. */
public record ArchitectureSubmissionResponse(
        Long submissionId,
        ArchitectFeedback coachFeedback,
        Instant createdAt
) {}
