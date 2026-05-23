package com.keepcoding.dto;

import com.keepcoding.domain.enums.Difficulty;

/** Resumo de um desafio de arquitetura para listagem. */
public record ArchitectureChallengeSummary(
        Long id,
        String title,
        Difficulty difficulty
) {}
