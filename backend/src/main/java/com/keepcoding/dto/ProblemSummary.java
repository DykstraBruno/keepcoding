package com.keepcoding.dto;

import com.keepcoding.domain.enums.Difficulty;

/** Resumo de um problema para listagem, incluindo se o usuário já o resolveu. */
public record ProblemSummary(
        Long id,
        String title,
        Difficulty difficulty,
        boolean solved
) {}
