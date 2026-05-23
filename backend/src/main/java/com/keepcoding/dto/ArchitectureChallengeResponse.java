package com.keepcoding.dto;

import com.keepcoding.domain.enums.Difficulty;

/** Detalhe completo de um desafio de arquitetura. */
public record ArchitectureChallengeResponse(
        Long id,
        String title,
        String context,
        String requirements,
        Difficulty difficulty
) {}
