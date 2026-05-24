package com.keepcoding.controller;

import com.keepcoding.dto.MatchForfeitRequest;
import com.keepcoding.dto.MatchStateDto;
import com.keepcoding.dto.MatchSubmitRequest;
import com.keepcoding.dto.QueueJoinRequest;
import com.keepcoding.service.MatchService;
import com.keepcoding.service.MatchmakingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints REST das partidas em tempo real (duelos). */
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchmakingService matchmakingService;
    private final MatchService matchService;

    /** Entra na fila de matchmaking da dificuldade escolhida. */
    @PostMapping("/queue")
    public ResponseEntity<Void> joinQueue(@Valid @RequestBody QueueJoinRequest request,
                                          @AuthenticationPrincipal UserDetails principal) {
        matchmakingService.join(request.difficulty(), principal.getUsername());
        return ResponseEntity.accepted().build();
    }

    /** Sai da fila (cancela o matchmaking em andamento). */
    @DeleteMapping("/queue")
    public ResponseEntity<Void> leaveQueue(@AuthenticationPrincipal UserDetails principal) {
        matchmakingService.leave(principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    /** Histórico de partidas do usuário autenticado. */
    @GetMapping
    public List<MatchStateDto> history(@AuthenticationPrincipal UserDetails principal) {
        return matchService.history(principal.getUsername());
    }

    /** Estado atual de uma partida. */
    @GetMapping("/{id}")
    public MatchStateDto get(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails principal) {
        return matchService.getState(id, principal.getUsername());
    }

    /** Envia uma tentativa de solução. Se for o primeiro ACCEPTED, vence. */
    @PostMapping("/{id}/submit")
    public ResponseEntity<MatchStateDto> submit(@PathVariable Long id,
                                                @Valid @RequestBody MatchSubmitRequest request,
                                                @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchService.submit(id, request, principal.getUsername()));
    }

    /**
     * Desiste — usado pelo frontend ao detectar violação de anti-cheat
     * (troca de aba, perda de foco, saída de fullscreen) ou desistência manual.
     */
    @PostMapping("/{id}/forfeit")
    public ResponseEntity<MatchStateDto> forfeit(@PathVariable Long id,
                                                 @Valid @RequestBody MatchForfeitRequest request,
                                                 @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(matchService.forfeit(id, request, principal.getUsername()));
    }
}
