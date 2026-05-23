package com.keepcoding.controller;

import com.keepcoding.dto.ArchitectureChallengeResponse;
import com.keepcoding.dto.ArchitectureChallengeSummary;
import com.keepcoding.dto.ArchitectureSubmissionRequest;
import com.keepcoding.dto.ArchitectureSubmissionResponse;
import com.keepcoding.service.ArchitectureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints dos desafios de arquitetura. */
@RestController
@RequestMapping("/api/architecture")
@RequiredArgsConstructor
public class ArchitectureController {

    private final ArchitectureService architectureService;

    /** Lista os desafios de arquitetura (resumo). */
    @GetMapping("/challenges")
    public List<ArchitectureChallengeSummary> listChallenges() {
        return architectureService.listChallenges();
    }

    /** Detalhe de um desafio de arquitetura. */
    @GetMapping("/challenges/{id}")
    public ArchitectureChallengeResponse getChallenge(@PathVariable Long id) {
        return architectureService.getChallenge(id);
    }

    /** Submete uma arquitetura proposta para análise do DevCoach. */
    @PostMapping("/submissions")
    public ResponseEntity<ArchitectureSubmissionResponse> submit(
            @Valid @RequestBody ArchitectureSubmissionRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(architectureService.submit(request, principal.getUsername()));
    }
}
