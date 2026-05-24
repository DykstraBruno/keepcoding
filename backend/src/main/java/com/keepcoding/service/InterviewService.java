package com.keepcoding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepcoding.domain.Interview;
import com.keepcoding.domain.InterviewMessage;
import com.keepcoding.domain.User;
import com.keepcoding.domain.enums.InterviewPhase;
import com.keepcoding.domain.enums.InterviewStatus;
import com.keepcoding.domain.enums.MessageRole;
import com.keepcoding.dto.*;
import com.keepcoding.repository.InterviewMessageRepository;
import com.keepcoding.repository.InterviewRepository;
import com.keepcoding.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Orquestra o ciclo de uma entrevista no formato padrão da indústria:
 * 40 minutos = 10 min de apresentação + 30 min de perguntas
 * (~4 min por par pergunta+resposta → ~7 perguntas).
 *
 * Fluxo:
 *  - start   : cria a sessão e o entrevistador pede a APRESENTAÇÃO.
 *  - answer  : registra a resposta. Se ainda dentro do bloco, pede a
 *              próxima pergunta; senão encerra e devolve o feedback.
 *  - list/get: leitura.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    /** Perguntas no bloco de Q&A (não conta a apresentação). 30 min / 4 min ≈ 7. */
    static final int DEFAULT_MAX_QUESTIONS = 7;
    static final int MIN_QUESTIONS = 4;
    static final int MAX_QUESTIONS_LIMIT = 10;

    /** 1 turno extra do candidato é reservado para a apresentação inicial. */
    private static final int PRESENTATION_TURNS = 1;

    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewMessageRepository messageRepository;
    private final InterviewerAiService interviewerAiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public InterviewTurnResponse start(StartInterviewRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userEmail));

        int max = clamp(
                request.maxQuestions() == null ? DEFAULT_MAX_QUESTIONS : request.maxQuestions(),
                MIN_QUESTIONS, MAX_QUESTIONS_LIMIT);

        Interview interview = interviewRepository.save(Interview.builder()
                .user(user)
                .targetRole(request.targetRole().trim())
                .resumeText(request.resumeText().trim())
                .maxQuestions(max)
                .status(InterviewStatus.IN_PROGRESS)
                .build());

        // 1ª mensagem do entrevistador = saudação + pedido de apresentação.
        String firstMessage = interviewerAiService.nextQuestion(interview, List.of());
        messageRepository.save(InterviewMessage.builder()
                .interview(interview)
                .role(MessageRole.INTERVIEWER)
                .turnIndex(0)
                .content(firstMessage)
                .build());

        return new InterviewTurnResponse(
                interview.getId(),
                interview.getStatus(),
                InterviewPhase.PRESENTATION,
                1,
                max,
                firstMessage,
                false,
                null);
    }

    @Transactional
    public InterviewTurnResponse answer(Long interviewId, AnswerRequest request, String userEmail) {
        Interview interview = loadOwned(interviewId, userEmail);
        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Esta entrevista já foi encerrada.");
        }

        List<InterviewMessage> history =
                messageRepository.findByInterviewIdOrderByTurnIndexAsc(interviewId);

        // 1) Salva a resposta do candidato como próximo turno.
        InterviewMessage answer = messageRepository.save(InterviewMessage.builder()
                .interview(interview)
                .role(MessageRole.CANDIDATE)
                .turnIndex(history.size())
                .content(request.content().trim())
                .build());
        history.add(answer);

        long answersGiven = history.stream()
                .filter(m -> m.getRole() == MessageRole.CANDIDATE)
                .count();
        int totalCandidateTurns = interview.getMaxQuestions() + PRESENTATION_TURNS;

        // 2) Última resposta esperada? Encerra com feedback final.
        if (answersGiven >= totalCandidateTurns) {
            InterviewFeedback feedback = interviewerAiService.produceFeedback(interview, history);
            interview.setFinalFeedbackJson(toJson(feedback));
            interview.setStatus(InterviewStatus.COMPLETED);
            interview.setFinishedAt(Instant.now());
            interviewRepository.save(interview);

            return new InterviewTurnResponse(
                    interview.getId(),
                    interview.getStatus(),
                    InterviewPhase.COMPLETED,
                    interview.getMaxQuestions(),
                    interview.getMaxQuestions(),
                    null,
                    true,
                    feedback);
        }

        // 3) Próxima pergunta do bloco.
        String nextQuestion = interviewerAiService.nextQuestion(interview, history);
        messageRepository.save(InterviewMessage.builder()
                .interview(interview)
                .role(MessageRole.INTERVIEWER)
                .turnIndex(history.size())
                .content(nextQuestion)
                .build());

        // Após a resposta de apresentação, entramos na fase de perguntas.
        // questionNumber dentro de QUESTIONS = answersGiven (já contando a apresentação).
        // Ex.: após responder apresentação (answersGiven=1) → fazendo Q1.
        int questionNumberInQuestions = (int) answersGiven;
        return new InterviewTurnResponse(
                interview.getId(),
                interview.getStatus(),
                InterviewPhase.QUESTIONS,
                questionNumberInQuestions,
                interview.getMaxQuestions(),
                nextQuestion,
                false,
                null);
    }

    @Transactional(readOnly = true)
    public List<InterviewSummary> list(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userEmail));

        return interviewRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(i -> {
                    long answered = messageRepository
                            .countByInterviewIdAndRole(i.getId(), MessageRole.CANDIDATE);
                    return new InterviewSummary(
                            i.getId(),
                            i.getTargetRole(),
                            i.getStatus(),
                            (int) answered,
                            i.getMaxQuestions(),
                            i.getCreatedAt(),
                            i.getFinishedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public InterviewDetail get(Long interviewId, String userEmail) {
        Interview interview = loadOwned(interviewId, userEmail);
        List<InterviewMessageView> messages = messageRepository
                .findByInterviewIdOrderByTurnIndexAsc(interviewId).stream()
                .map(m -> new InterviewMessageView(
                        m.getRole(), m.getTurnIndex(), m.getContent(), m.getCreatedAt()))
                .toList();

        InterviewPhase phase = computePhase(interview, messages);

        return new InterviewDetail(
                interview.getId(),
                interview.getTargetRole(),
                interview.getResumeText(),
                interview.getStatus(),
                phase,
                interview.getMaxQuestions(),
                messages,
                fromJson(interview.getFinalFeedbackJson()),
                interview.getCreatedAt(),
                interview.getFinishedAt());
    }

    // ---------------------------------------------------------------- helpers
    private InterviewPhase computePhase(Interview interview, List<InterviewMessageView> messages) {
        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            return InterviewPhase.COMPLETED;
        }
        long answersGiven = messages.stream()
                .filter(m -> m.role() == MessageRole.CANDIDATE).count();
        // 0 respostas = ainda na apresentação (entrevistador acabou de pedir).
        return answersGiven == 0 ? InterviewPhase.PRESENTATION : InterviewPhase.QUESTIONS;
    }

    private Interview loadOwned(Long id, String userEmail) {
        Interview interview = interviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entrevista não encontrada: " + id));
        if (!interview.getUser().getEmail().equals(userEmail)) {
            // Não vaza existência de entrevista de outro usuário.
            throw new EntityNotFoundException("Entrevista não encontrada: " + id);
        }
        return interview;
    }

    private static int clamp(int value, int lo, int hi) {
        return Math.max(lo, Math.min(hi, value));
    }

    private String toJson(InterviewFeedback feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (JsonProcessingException e) {
            log.warn("Falha ao serializar feedback de entrevista.", e);
            return "{}";
        }
    }

    private InterviewFeedback fromJson(String json) {
        if (json == null || json.isBlank() || "{}".equals(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, InterviewFeedback.class);
        } catch (Exception e) {
            log.warn("Falha ao parsear feedback de entrevista.", e);
            return null;
        }
    }
}
