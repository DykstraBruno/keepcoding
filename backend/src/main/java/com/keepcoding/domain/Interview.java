package com.keepcoding.domain;

import com.keepcoding.domain.enums.InterviewStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Sessão de entrevista de um usuário com a IA do KeepCoding.
 * O currículo e a vaga-alvo definem o contexto que a IA usa para
 * personalizar cada pergunta.
 */
@Entity
@Table(name = "interviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Vaga / posição que o candidato está mirando (ex.: "Backend Java Pleno"). */
    @Column(name = "target_role", nullable = false, length = 255)
    private String targetRole;

    /** Currículo em texto puro fornecido pelo candidato. */
    @Column(name = "resume_text", nullable = false, columnDefinition = "TEXT")
    private String resumeText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InterviewStatus status = InterviewStatus.IN_PROGRESS;

    /** Quantidade máxima de perguntas planejadas para esta sessão. */
    @Column(name = "max_questions", nullable = false)
    @Builder.Default
    private Integer maxQuestions = 7;

    /** Feedback final do entrevistador, em JSON. Preenchido ao concluir. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "final_feedback_json", columnDefinition = "jsonb")
    private String finalFeedbackJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
