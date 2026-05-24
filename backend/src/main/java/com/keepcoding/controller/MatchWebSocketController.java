package com.keepcoding.controller;

import com.keepcoding.dto.QueueJoinRequest;
import com.keepcoding.service.MatchmakingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * Endpoints STOMP da fila de matchmaking.
 *
 * <p>Clientes enviam para {@code /app/queue/join} com {@link QueueJoinRequest}
 * e recebem em {@code /user/queue/match} (status) e {@code /user/queue/match-found}
 * (quando pareados). Em paralelo, REST também aceita a entrada via
 * POST /api/matches/queue — qualquer dos dois caminhos funciona.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MatchWebSocketController {

    private final MatchmakingService matchmakingService;

    @MessageMapping("/queue/join")
    public void join(@Valid @Payload QueueJoinRequest request,
                     @AuthenticationPrincipal Principal principal) {
        if (principal == null) {
            log.warn("[Matchmaking] CONNECT sem principal — ignorando join.");
            return;
        }
        matchmakingService.join(request.difficulty(), principal.getName());
    }

    @MessageMapping("/queue/leave")
    public void leave(@AuthenticationPrincipal Principal principal) {
        if (principal == null) {
            return;
        }
        matchmakingService.leave(principal.getName());
    }
}
