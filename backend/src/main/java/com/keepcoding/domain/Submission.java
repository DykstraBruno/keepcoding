package com.keepcoding.domain;

import com.keepcoding.domain.enums.Language;
import com.keepcoding.domain.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** Tentativa de resolucao enviada por um usuario para um problema. */
@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.PENDING;

    /** Feedback do DevCoach serializado como JSON (coluna jsonb). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coach_feedback_json", columnDefinition = "jsonb")
    private String coachFeedbackJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
