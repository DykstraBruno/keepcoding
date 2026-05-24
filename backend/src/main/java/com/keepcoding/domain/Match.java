package com.keepcoding.domain;

import com.keepcoding.domain.enums.Difficulty;
import com.keepcoding.domain.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Duelo em tempo real entre dois usuários sobre um único problema.
 * O primeiro a obter ACCEPTED é o vencedor.
 */
@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player1_id", nullable = false)
    private User player1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player2_id", nullable = false)
    private User player2;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MatchStatus status = MatchStatus.ACTIVE;

    /** Vencedor; nulo enquanto a partida está em andamento. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    /** Motivo do encerramento quando ABANDONED (ex.: "tab_switch"). */
    @Column(name = "forfeit_reason", length = 64)
    private String forfeitReason;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;
}
