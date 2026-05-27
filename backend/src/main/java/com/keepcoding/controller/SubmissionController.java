package com.keepcoding.controller;

import com.keepcoding.dto.SubmissionRequest;
import com.keepcoding.dto.SubmissionResponse;
import com.keepcoding.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/** Endpoints de submissao de codigo. */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    /**
     * Recebe o codigo do usuario, executa no sandbox e devolve
     * o veredito junto com o feedback do DevCoach.
     */
    @PostMapping
    public ResponseEntity<SubmissionResponse> submit(
            @Valid @RequestBody SubmissionRequest request,
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "X-OpenAI-Key", required = false) String openAiKey) {
        // principal.getUsername() == e-mail do usuário autenticado (subject do JWT).
        // openAiKey: BYOK opcional vindo do header (frontend lê do localStorage).
        SubmissionResponse response = submissionService.submit(
                request, principal.getUsername(), openAiKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
