package com.keepcoding.domain;

import com.keepcoding.domain.enums.Difficulty;
import jakarta.persistence.*;
import lombok.*;

/**
 * Desafio de arquitetura de software: um contexto/cenário para o qual
 * o usuário deve propor uma arquitetura (diagrama Mermaid + justificativas).
 */
@Entity
@Table(name = "architecture_challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectureChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /** Cenário de negócio a ser resolvido. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String context;

    /** Requisitos / restrições que a arquitetura deve atender. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirements;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;
}
