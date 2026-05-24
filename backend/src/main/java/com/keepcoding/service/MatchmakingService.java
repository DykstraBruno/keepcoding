package com.keepcoding.service;

import com.keepcoding.domain.Match;
import com.keepcoding.domain.Problem;
import com.keepcoding.domain.User;
import com.keepcoding.domain.enums.Difficulty;
import com.keepcoding.domain.enums.MatchStatus;
import com.keepcoding.dto.MatchStateDto;
import com.keepcoding.dto.QueueStatusDto;
import com.keepcoding.repository.MatchRepository;
import com.keepcoding.repository.ProblemRepository;
import com.keepcoding.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Fila de matchmaking em memória, particionada por dificuldade.
 *
 * Quando dois usuários distintos estão na mesma fila eles são pareados,
 * uma {@link Match} é criada (status ACTIVE) e os dois recebem o evento
 * {@code /user/queue/match-found} via STOMP. O cliente então navega para
 * a tela do duelo e assina {@code /topic/match/{matchId}}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final MatchRepository matchRepository;
    private final MatchProjection matchProjection;
    private final SimpMessagingTemplate messaging;

    /** Uma fila por dificuldade. ConcurrentLinkedDeque pra entrada/saída thread-safe. */
    private final ConcurrentHashMap<Difficulty, ConcurrentLinkedDeque<QueueEntry>> queues =
            new ConcurrentHashMap<>();

    private final Random random = new Random();

    /** Entrada da fila — armazena identidade do usuário pra evitar pareamento com ele mesmo. */
    private record QueueEntry(Long userId, String email, String username) {}

    @Transactional
    public void join(Difficulty difficulty, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado: " + userEmail));

        ConcurrentLinkedDeque<QueueEntry> queue =
                queues.computeIfAbsent(difficulty, d -> new ConcurrentLinkedDeque<>());

        // Tira da fila qualquer entrada antiga deste mesmo usuário (evita re-fila duplicada).
        queue.removeIf(e -> e.userId().equals(user.getId()));

        QueueEntry opponent = queue.poll();
        while (opponent != null && opponent.userId().equals(user.getId())) {
            opponent = queue.poll();
        }

        if (opponent == null) {
            queue.offer(new QueueEntry(user.getId(), user.getEmail(), user.getUsername()));
            messaging.convertAndSendToUser(user.getEmail(), "/queue/match",
                    new QueueStatusDto("WAITING", difficulty,
                            "Procurando oponente para o nível " + difficulty + "…"));
            log.info("[Matchmaking] {} entrou na fila {}.", user.getUsername(), difficulty);
            return;
        }

        // Há oponente — formar a partida.
        final QueueEntry pairedOpponent = opponent;
        User opponentUser = userRepository.findById(pairedOpponent.userId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Oponente sumiu: " + pairedOpponent.userId()));

        Problem problem = pickRandomProblem(difficulty);
        Match match = matchRepository.save(Match.builder()
                .player1(opponentUser)
                .player2(user)
                .problem(problem)
                .difficulty(difficulty)
                .status(MatchStatus.ACTIVE)
                .startedAt(Instant.now())
                .build());

        MatchStateDto state = matchProjection.toState(match);
        messaging.convertAndSendToUser(opponent.email(), "/queue/match-found", state);
        messaging.convertAndSendToUser(user.getEmail(), "/queue/match-found", state);
        log.info("[Matchmaking] Partida {} criada: {} x {} ({}, problema #{})",
                match.getId(), opponent.username(), user.getUsername(), difficulty, problem.getId());
    }

    public void leave(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return;
        }
        queues.values().forEach(q -> q.removeIf(e -> Objects.equals(e.userId(), user.getId())));
        messaging.convertAndSendToUser(user.getEmail(), "/queue/match",
                new QueueStatusDto("LEFT", null, "Você saiu da fila."));
        log.info("[Matchmaking] {} saiu da fila.", user.getUsername());
    }

    private Problem pickRandomProblem(Difficulty difficulty) {
        List<Problem> pool = problemRepository.findByDifficulty(difficulty);
        if (pool.isEmpty()) {
            throw new IllegalStateException("Nenhum problema disponível para " + difficulty);
        }
        return pool.get(random.nextInt(pool.size()));
    }
}
