package com.keepcoding.dto;

import com.keepcoding.domain.enums.InterviewStatus;

import java.time.Instant;
import java.util.List;

/** Detalhe completo de uma entrevista: contexto, transcrição e feedback. */
public record InterviewDetail(
        Long id,
        String targetRole,
        String resumeText,
        InterviewStatus status,
        Integer maxQuestions,
        List<InterviewMessageView> messages,
        InterviewFeedback feedback,
        Instant createdAt,
        Instant finishedAt
) {}
