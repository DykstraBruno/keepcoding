package com.keepcoding.dto;

/** Caso de teste exposto ao frontend (apenas exemplos visíveis). */
public record TestCaseView(
        Long id,
        String input,
        String expectedOutput,
        boolean isSample
) {}
