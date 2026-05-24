package com.keepcoding.dto;

import java.util.List;

/**
 * Resposta do GET /api/ranking.
 *
 * @param top         primeiras N posições (default 50)
 * @param me          posição do usuário autenticado quando ele está fora do top;
 *                    null se já aparece dentro de {@link #top}
 * @param totalUsers  tamanho total da tabela (todos com conta criada)
 */
public record RankingResponse(
        List<RankingEntry> top,
        RankingEntry me,
        Integer totalUsers
) {}
