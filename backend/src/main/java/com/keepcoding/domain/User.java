package com.keepcoding.domain;

import com.keepcoding.domain.enums.TierPlan;
import jakarta.persistence.*;
import lombok.*;

/**
 * Usuario da plataforma. Mapeado para a tabela "users"
 * ("user" e palavra reservada no PostgreSQL).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /** Armazenar SEMPRE o hash (BCrypt), nunca texto puro. */
    @Column(nullable = false)
    private String password;

    /** Pontos de experiencia acumulados. */
    @Column(nullable = false)
    @Builder.Default
    private Integer xp = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier_plan", nullable = false, length = 20)
    @Builder.Default
    private TierPlan tierPlan = TierPlan.FREE;
}
