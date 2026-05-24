package com.keepcoding.dto;

import com.keepcoding.domain.enums.MatchEventType;
import com.keepcoding.domain.enums.SubmissionStatus;

import java.time.Instant;

/**
 * Evento de partida propagado via STOMP em {@code /topic/match/{id}}.
 * Campos extras só são preenchidos quando relevantes ao tipo do evento.
 */
public record MatchEventDto(
        MatchEventType type,
        PlayerView actor,           // jogador que disparou (submit/forfeit); null para MATCH_ENDED
        SubmissionStatus result,    // status da submissão (apenas SUBMISSION/WIN)
        Integer passedTests,        // apenas SUBMISSION/WIN
        Integer totalTests,         // apenas SUBMISSION/WIN
        PlayerView winner,          // apenas WIN/MATCH_ENDED
        String forfeitReason,       // apenas FORFEIT/MATCH_ENDED quando aplicável
        Instant timestamp
) {}
