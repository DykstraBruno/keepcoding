package com.keepcoding.domain;

import com.keepcoding.domain.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/** Desafio de algoritmo (ex.: "Two Sum"). */
@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    /** Tempo limite de execucao em milissegundos. */
    @Column(name = "time_limit", nullable = false)
    @Builder.Default
    private Integer timeLimit = 2000;

    /** Memoria limite em kilobytes. */
    @Column(name = "memory_limit", nullable = false)
    @Builder.Default
    private Integer memoryLimit = 128000;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();
}
