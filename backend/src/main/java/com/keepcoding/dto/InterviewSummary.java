package com.keepcoding.dto;

import com.keepcoding.domain.enums.InterviewStatus;

import java.time.Instant;

/** Resumo de uma entrevista para listagem. */
public record InterviewSummary(
        Long id,
        String targetRole,
        InterviewStatus status,
        Integer questionsAnswered,
        Integer totalQuestions,
        Instant createdAt,
        Instant finishedAt
) {}
