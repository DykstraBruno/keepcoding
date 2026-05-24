package com.keepcoding.service;

import com.keepcoding.domain.Match;
import com.keepcoding.domain.User;
import com.keepcoding.domain.enums.MatchEventType;
import com.keepcoding.domain.enums.MatchStatus;
import com.keepcoding.domain.enums.SubmissionStatus;
import com.keepcoding.dto.MatchEventDto;
import com.keepcoding.dto.MatchForfeitRequest;
import com.keepcoding.dto.MatchStateDto;
import com.keepcoding.dto.MatchSubmitRequest;
import com.keepcoding.dto.PlayerView;
import com.keepcoding.repository.MatchRepository;
import com.keepcoding.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Lógica de uma partida em andamento: submissão de código, forfeit por
 * violação de anti-cheat ou desistência, e leitura de estado/histórico.
 *
 * O primeiro jogador a obter {@link SubmissionStatus#ACCEPTED} vence.
 * Submissões posteriores em uma partida já finalizada são rejeitadas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final Judge0Service judge0Service;
    private final SimpMessagingTemplate messaging;
    private final MatchProjection matchProjection;

    @Transactional
    public MatchStateDto submit(Long matchId, MatchSubmitRequest request, String userEmail) {
        Match match = loadOwned(matchId, userEmail);
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new IllegalArgumentException("Esta partida já foi encerrada.");
        }
        User actor = currentPlayer(match, userEmail);

        Judge0Service.ExecutionResult execution =
                judge0Service.execute(match.getProblem(), request.language(), request.code());

        Instant now = Instant.now();
        boolean accepted = execution.status() == SubmissionStatus.ACCEPTED;

        if (accepted) {
            match.setStatus(MatchStatus.COMPLETED);
            match.setWinner(actor);
            match.setEndedAt(now);
            matchRepository.save(match);
            broadcast(matchId, new MatchEventDto(
                    MatchEventType.WIN,
                    PlayerView.from(actor),
                    execution.status(), execution.passedTests(), execution.totalTests(),
                    PlayerView.from(actor),
                    null, now));
            broadcast(matchId, endedEvent(match, now));
            log.info("[Match] Partida {} vencida por {} ({})", matchId, actor.getUsername(),
                    execution.status());
        } else {
            broadcast(matchId, new MatchEventDto(
                    MatchEventType.SUBMISSION,
                    PlayerView.from(actor),
                    execution.status(), execution.passedTests(), execution.totalTests(),
                    null, null, now));
            log.info("[Match] {} submeteu em #{}: {} ({}/{})", actor.getUsername(), matchId,
                    execution.status(), execution.passedTests(), execution.totalTests());
        }

        return matchProjection.toState(match);
    }

    @Transactional
    public MatchStateDto forfeit(Long matchId, MatchForfeitRequest request, String userEmail) {
        Match match = loadOwned(matchId, userEmail);
        if (match.getStatus() != MatchStatus.ACTIVE) {
            return matchProjection.toState(match);
        }
        User actor = currentPlayer(match, userEmail);
        User opponent = match.getPlayer1().getId().equals(actor.getId())
                ? match.getPlayer2()
                : match.getPlayer1();

        Instant now = Instant.now();
        match.setStatus(MatchStatus.ABANDONED);
        match.setWinner(opponent);
        match.setForfeitReason(request.reason());
        match.setEndedAt(now);
        matchRepository.save(match);

        broadcast(matchId, new MatchEventDto(
                MatchEventType.FORFEIT,
                PlayerView.from(actor),
                null, null, null,
                PlayerView.from(opponent),
                request.reason(),
                now));
        broadcast(matchId, endedEvent(match, now));
        log.info("[Match] {} forfeitou #{} (motivo='{}'); vencedor: {}",
                actor.getUsername(), matchId, request.reason(), opponent.getUsername());

        return matchProjection.toState(match);
    }

    @Transactional(readOnly = true)
    public MatchStateDto getState(Long matchId, String userEmail) {
        return matchProjection.toState(loadOwned(matchId, userEmail));
    }

    @Transactional(readOnly = true)
    public List<MatchStateDto> history(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userEmail));
        return matchRepository
                .findByPlayer1IdOrPlayer2IdOrderByStartedAtDesc(user.getId(), user.getId())
                .stream()
                .map(matchProjection::toState)
                .toList();
    }

    // ---------------------------------------------------------------- helpers
    private MatchEventDto endedEvent(Match match, Instant now) {
        return new MatchEventDto(
                MatchEventType.MATCH_ENDED,
                null,
                null, null, null,
                match.getWinner() == null ? null : PlayerView.from(match.getWinner()),
                match.getForfeitReason(),
                now);
    }

    private void broadcast(Long matchId, MatchEventDto event) {
        messaging.convertAndSend("/topic/match/" + matchId, event);
    }

    private Match loadOwned(Long id, String userEmail) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Partida não encontrada: " + id));
        boolean isPlayer = match.getPlayer1().getEmail().equals(userEmail)
                || match.getPlayer2().getEmail().equals(userEmail);
        if (!isPlayer) {
            // Não vaza existência da partida para terceiros.
            throw new EntityNotFoundException("Partida não encontrada: " + id);
        }
        return match;
    }

    private User currentPlayer(Match match, String userEmail) {
        return match.getPlayer1().getEmail().equals(userEmail)
                ? match.getPlayer1()
                : match.getPlayer2();
    }
}
