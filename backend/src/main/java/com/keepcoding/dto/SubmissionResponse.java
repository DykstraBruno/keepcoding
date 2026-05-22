package com.keepcoding.dto;

import com.keepcoding.domain.enums.SubmissionStatus;

import java.time.Instant;

/** Resposta devolvida apos processar uma submissao. */
public record SubmissionResponse(
        Long submissionId,
        SubmissionStatus status,
        Integer passedTests,
        Integer totalTests,
        CoachFeedback coachFeedback,
        Instant createdAt
) {}
