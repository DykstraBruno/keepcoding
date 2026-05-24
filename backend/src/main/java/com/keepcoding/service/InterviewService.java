package com.keepcoding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepcoding.domain.Interview;
import com.keepcoding.domain.InterviewMessage;
import com.keepcoding.domain.User;
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
 * Orquestra o ciclo de vida de uma entrevista:
 *  - start   : valida currículo, cria a sessão e pede a 1ª pergunta à IA;
 *  - answer  : registra a resposta do candidato, pede a próxima pergunta
 *              ou encerra com o feedback final;
 *  - list/get: leitura para o frontend.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    static final int DEFAULT_MAX_QUESTIONS = 6;
    static final int MIN_QUESTIONS = 3;
    static final int MAX_QUESTIONS_LIMIT = 12;

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

        // Pede a primeira pergunta (history vazio).
        String firstQuestion = interviewerAiService.nextQuestion(interview, List.of());
        messageRepository.save(InterviewMessage.builder()
                .interview(interview)
                .role(MessageRole.INTERVIEWER)
                .turnIndex(0)
                .content(firstQuestion)
                .build());

        return new InterviewTurnResponse(
                interview.getId(),
                interview.getStatus(),
                1,
                max,
                firstQuestion,
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

        long questionsAsked = history.stream().filter(m -> m.getRole() == MessageRole.INTERVIEWER).count();
        long answersGiven  = history.stream().filter(m -> m.getRole() == MessageRole.CANDIDATE).count();

        // 2) Atingiu o limite de respostas? Encerra a entrevista com feedback final.
        if (answersGiven >= interview.getMaxQuestions()) {
            InterviewFeedback feedback = interviewerAiService.produceFeedback(interview, history);
            interview.setFinalFeedbackJson(toJson(feedback));
            interview.setStatus(InterviewStatus.COMPLETED);
            interview.setFinishedAt(Instant.now());
            interviewRepository.save(interview);

            return new InterviewTurnResponse(
                    interview.getId(),
                    interview.getStatus(),
                    (int) questionsAsked,
                    interview.getMaxQuestions(),
                    null,
                    true,
                    feedback);
        }

        // 3) Caso contrário, pede a próxima pergunta à IA.
        String nextQuestion = interviewerAiService.nextQuestion(interview, history);
        messageRepository.save(InterviewMessage.builder()
                .interview(interview)
                .role(MessageRole.INTERVIEWER)
                .turnIndex(history.size())
                .content(nextQuestion)
                .build());

        return new InterviewTurnResponse(
                interview.getId(),
                interview.getStatus(),
                (int) questionsAsked + 1,
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

        return new InterviewDetail(
                interview.getId(),
                interview.getTargetRole(),
                interview.getResumeText(),
                interview.getStatus(),
                interview.getMaxQuestions(),
                messages,
                fromJson(interview.getFinalFeedbackJson()),
                interview.getCreatedAt(),
                interview.getFinishedAt());
    }

    // ---------------------------------------------------------------- helpers
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
