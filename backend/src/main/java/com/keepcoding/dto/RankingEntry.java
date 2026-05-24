package com.keepcoding.dto;

/** Uma linha do ranking entregue ao frontend (com posição 1-indexada). */
public record RankingEntry(
        Integer position,
        Long userId,
        String username,
        Integer xp,
        Long easyCount,
        Long mediumCount,
        Long hardCount
) {}
