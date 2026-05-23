package com.keepcoding.dto;

import com.keepcoding.domain.enums.Difficulty;

import java.util.List;

/** Detalhe completo de um problema (com os casos de teste de exemplo). */
public record ProblemDetailResponse(
        Long id,
        String title,
        String description,
        Difficulty difficulty,
        Integer timeLimit,
        Integer memoryLimit,
        List<TestCaseView> testCases
) {}
