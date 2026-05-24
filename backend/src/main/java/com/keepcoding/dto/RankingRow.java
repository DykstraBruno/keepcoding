package com.keepcoding.dto;

/**
 * Projeção crua do agregado por usuário usado pela query do ranking.
 * Não vai pro frontend — virá {@link RankingEntry} com a posição calculada.
 */
public record RankingRow(
        Long userId,
        String username,
        Integer xp,
        Long easyCount,
        Long mediumCount,
        Long hardCount
) {}
