package com.keepcoding.dto;

import com.keepcoding.domain.enums.Difficulty;
import com.keepcoding.domain.enums.MatchStatus;

import java.time.Instant;

/**
 * Estado completo de uma partida para o frontend.
 * Usado tanto na entrega via WebSocket (match-found) quanto no GET de detalhe.
 */
public record MatchStateDto(
        Long matchId,
        Difficulty difficulty,
        MatchStatus status,
        PlayerView player1,
        PlayerView player2,
        PlayerView winner,
        String forfeitReason,
        ProblemDetailResponse problem,
        Instant startedAt,
        Instant endedAt
) {}
