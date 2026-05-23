package com.keepcoding.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** Arquitetura proposta por um usuário para um desafio de arquitetura. */
@Entity
@Table(name = "architecture_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectureSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private ArchitectureChallenge challenge;

    /** Diagrama da arquitetura em sintaxe Mermaid. */
    @Column(name = "mermaid_code", nullable = false, columnDefinition = "TEXT")
    private String mermaidCode;

    /** Justificativas e decisões de projeto descritas pelo usuário. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Feedback do DevCoach (análise de arquitetura) em JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coach_feedback_json", columnDefinition = "jsonb")
    private String coachFeedbackJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
