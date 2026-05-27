package com.keepcoding.controller;

import com.keepcoding.dto.AnswerRequest;
import com.keepcoding.dto.InterviewDetail;
import com.keepcoding.dto.InterviewSummary;
import com.keepcoding.dto.InterviewTurnResponse;
import com.keepcoding.dto.StartInterviewRequest;
import com.keepcoding.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints das entrevistas com a IA. */
@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /** Inicia uma nova entrevista e devolve a primeira pergunta da IA. */
    @PostMapping("/start")
    public ResponseEntity<InterviewTurnResponse> start(
            @Valid @RequestBody StartInterviewRequest request,
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "X-OpenAI-Key", required = false) String openAiKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewService.start(request, principal.getUsername(), openAiKey));
    }

    /** Envia uma resposta; devolve a próxima pergunta ou o feedback final. */
    @PostMapping("/{id}/answer")
    public ResponseEntity<InterviewTurnResponse> answer(
            @PathVariable Long id,
            @Valid @RequestBody AnswerRequest request,
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "X-OpenAI-Key", required = false) String openAiKey) {
        return ResponseEntity.ok(
                interviewService.answer(id, request, principal.getUsername(), openAiKey));
    }

    /** Histórico das entrevistas do usuário autenticado. */
    @GetMapping
    public List<InterviewSummary> list(@AuthenticationPrincipal UserDetails principal) {
        return interviewService.list(principal.getUsername());
    }

    /** Detalhe completo de uma entrevista (transcrição + feedback). */
    @GetMapping("/{id}")
    public InterviewDetail get(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails principal) {
        return interviewService.get(id, principal.getUsername());
    }
}
