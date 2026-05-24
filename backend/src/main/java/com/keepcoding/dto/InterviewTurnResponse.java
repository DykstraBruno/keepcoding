package com.keepcoding.dto;

import com.keepcoding.domain.enums.InterviewPhase;
import com.keepcoding.domain.enums.InterviewStatus;

/**
 * Resposta de POST /interviews/start e POST /interviews/{id}/answer.
 * Carrega a próxima pergunta OU o feedback final quando a entrevista encerra.
 */
public record InterviewTurnResponse(
        Long interviewId,
        InterviewStatus status,
        /** Fase atual: PRESENTATION (apresentação), QUESTIONS (bloco de perguntas) ou COMPLETED. */
        InterviewPhase phase,
        /** Número da pergunta atual dentro do bloco (1-indexed); 1 também na apresentação. */
        Integer questionNumber,
        /** Total de perguntas planejado para o bloco de Q&A (não inclui a apresentação). */
        Integer totalQuestions,
        /** Próxima mensagem do entrevistador; null quando finalizada. */
        String nextQuestion,
        /** true quando a entrevista foi encerrada nesta chamada. */
        boolean finished,
        /** Feedback final, presente apenas quando finished = true. */
        InterviewFeedback feedback
) {}
