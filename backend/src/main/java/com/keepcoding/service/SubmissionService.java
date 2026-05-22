package com.keepcoding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepcoding.domain.Problem;
import com.keepcoding.domain.Submission;
import com.keepcoding.domain.User;
import com.keepcoding.domain.enums.SubmissionStatus;
import com.keepcoding.dto.CoachFeedback;
import com.keepcoding.dto.SubmissionRequest;
import com.keepcoding.dto.SubmissionResponse;
import com.keepcoding.repository.ProblemRepository;
import com.keepcoding.repository.SubmissionRepository;
import com.keepcoding.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra o fluxo de submissao de codigo:
 * <ol>
 *   <li>Persiste a submissao como PENDING.</li>
 *   <li>Executa o codigo no sandbox (Judge0).</li>
 *   <li>Pede ao DevCoach a analise de qualidade.</li>
 *   <li>Salva o resultado e devolve a resposta.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final Judge0Service judge0Service;
    private final CoachAiService coachAiService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SubmissionResponse submit(SubmissionRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuario nao encontrado: " + userEmail));
        Problem problem = problemRepository.findById(request.problemId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Problema nao encontrado: " + request.problemId()));

        // 1. Persiste como PENDING
        Submission submission = submissionRepository.save(Submission.builder()
                .user(user)
                .problem(problem)
                .language(request.language())
                .code(request.code())
                .status(SubmissionStatus.PENDING)
                .build());

        // 2. Executa no sandbox
        Judge0Service.ExecutionResult execution =
                judge0Service.execute(problem, request.language(), request.code());
        submission.setStatus(execution.status());

        // 3. DevCoach analisa a qualidade da solucao
        boolean accepted = execution.status() == SubmissionStatus.ACCEPTED;
        CoachFeedback feedback = coachAiService.analyze(
                problem, request.language(), request.code(), accepted);

        // 4. Serializa o feedback e persiste o resultado final
        submission.setCoachFeedbackJson(toJson(feedback));
        submission = submissionRepository.save(submission);

        return new SubmissionResponse(
                submission.getId(),
                submission.getStatus(),
                execution.passedTests(),
                execution.totalTests(),
                feedback,
                submission.getCreatedAt());
    }

    private String toJson(CoachFeedback feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (JsonProcessingException e) {
            log.warn("Falha ao serializar feedback do DevCoach.", e);
            return "{}";
        }
    }
}
