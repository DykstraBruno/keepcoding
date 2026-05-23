package com.keepcoding.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Marca que um usuário resolveu um problema. A constraint única
 * (user_id, problem_id) garante que XP só seja concedido uma vez.
 */
@Entity
@Table(
        name = "solved_problems",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_solved_problems_user_problem",
                columnNames = {"user_id", "problem_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolvedProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    /** XP creditado neste primeiro acerto (snapshot, não muda se a tabela de XP for alterada). */
    @Column(name = "xp_awarded", nullable = false)
    private Integer xpAwarded;

    @CreationTimestamp
    @Column(name = "solved_at", nullable = false, updatable = false)
    private Instant solvedAt;
}
