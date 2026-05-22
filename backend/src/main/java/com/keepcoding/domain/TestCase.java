package com.keepcoding.domain;

import jakarta.persistence.*;
import lombok.*;

/** Caso de teste (entrada + saida esperada) de um problema. */
@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String input;

    @Column(name = "expected_output", nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    /** true = exemplo visivel ao usuario; false = teste oculto de validacao. */
    @Column(name = "is_sample", nullable = false)
    @Builder.Default
    private Boolean isSample = false;
}
