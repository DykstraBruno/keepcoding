package com.keepcoding.dto;

import com.keepcoding.domain.enums.InterviewStatus;

/**
 * Resposta de POST /interviews/start e POST /interviews/{id}/answer.
 * Carrega a próxima pergunta OU o feedback final quando a entrevista encerra.
 */
public record InterviewTurnResponse(
        Long interviewId,
        InterviewStatus status,
        /** Número da pergunta atual (1-indexed). */
        Integer questionNumber,
        /** Total de perguntas planejado para a sessão. */
        Integer totalQuestions,
        /** Próxima pergunta a ser exibida ao candidato; null quando finalizada. */
        String nextQuestion,
        /** true quando a entrevista foi encerrada nesta chamada. */
        boolean finished,
        /** Feedback final, presente apenas quando finished = true. */
        InterviewFeedback feedback
) {}
