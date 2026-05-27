package com.keepcoding.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keepcoding.domain.ArchitectureChallenge;
import com.keepcoding.domain.ArchitectureSubmission;
import com.keepcoding.domain.User;
import com.keepcoding.dto.*;
import com.keepcoding.repository.ArchitectureChallengeRepository;
import com.keepcoding.repository.ArchitectureSubmissionRepository;
import com.keepcoding.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Orquestra os desafios de arquitetura e suas submissões. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchitectureService {

    private final ArchitectureChallengeRepository challengeRepository;
    private final ArchitectureSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ArchitectCoachService architectCoachService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ArchitectureChallengeSummary> listChallenges() {
        return challengeRepository.findAll().stream()
                .sorted(Comparator.comparing(ArchitectureChallenge::getDifficulty)
                        .thenComparing(ArchitectureChallenge::getTitle))
                .map(c -> new ArchitectureChallengeSummary(c.getId(), c.getTitle(), c.getDifficulty()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ArchitectureChallengeResponse getChallenge(Long id) {
        ArchitectureChallenge c = challengeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Desafio não encontrado: " + id));
        return new ArchitectureChallengeResponse(
                c.getId(), c.getTitle(), c.getContext(), c.getRequirements(), c.getDifficulty());
    }

    /** Recebe a arquitetura proposta, pede a análise do DevCoach e persiste. */
    @Transactional
    public ArchitectureSubmissionResponse submit(ArchitectureSubmissionRequest request,
                                                 String userEmail, String userApiKey) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userEmail));
        ArchitectureChallenge challenge = challengeRepository.findById(request.challengeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Desafio não encontrado: " + request.challengeId()));

        ArchitectFeedback feedback = architectCoachService.analyze(
                challenge, request.mermaidCode(), request.notes(), userApiKey);

        ArchitectureSubmission submission = submissionRepository.save(ArchitectureSubmission.builder()
                .user(user)
                .challenge(challenge)
                .mermaidCode(request.mermaidCode())
                .notes(request.notes())
                .coachFeedbackJson(toJson(feedback))
                .build());

        return new ArchitectureSubmissionResponse(
                submission.getId(), feedback, submission.getCreatedAt());
    }

    private String toJson(ArchitectFeedback feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (JsonProcessingException e) {
            log.warn("Falha ao serializar feedback de arquitetura.", e);
            return "{}";
        }
    }
}
