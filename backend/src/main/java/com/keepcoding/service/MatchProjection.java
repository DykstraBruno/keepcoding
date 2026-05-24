package com.keepcoding.service;

import com.keepcoding.domain.Match;
import com.keepcoding.dto.MatchStateDto;
import com.keepcoding.dto.PlayerView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Converte {@link Match} no DTO completo de estado, incluindo o detalhe
 * do problema (com casos de exemplo). Extraído do {@code MatchService}
 * para evitar dependência circular com o {@code MatchmakingService}.
 */
@Component
@RequiredArgsConstructor
public class MatchProjection {

    private final ProblemQueryService problemQueryService;

    public MatchStateDto toState(Match match) {
        return new MatchStateDto(
                match.getId(),
                match.getDifficulty(),
                match.getStatus(),
                player(match.getPlayer1().getId(), match.getPlayer1().getUsername()),
                player(match.getPlayer2().getId(), match.getPlayer2().getUsername()),
                match.getWinner() == null ? null
                        : player(match.getWinner().getId(), match.getWinner().getUsername()),
                match.getForfeitReason(),
                problemQueryService.getById(match.getProblem().getId()),
                match.getStartedAt(),
                match.getEndedAt());
    }

    private static PlayerView player(Long id, String username) {
        return new PlayerView(id, username);
    }
}
