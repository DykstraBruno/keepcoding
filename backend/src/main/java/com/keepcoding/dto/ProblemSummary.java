package com.keepcoding.dto;

import com.keepcoding.domain.enums.Difficulty;

/** Resumo de um problema para listagem. */
public record ProblemSummary(
        Long id,
        String title,
        Difficulty difficulty
) {}
