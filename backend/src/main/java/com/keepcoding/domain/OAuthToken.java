package com.keepcoding.domain;

import com.keepcoding.domain.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Token OAuth de um usuário em um provider externo (Google/etc.).
 * Access token e refresh token são guardados criptografados (AES-GCM).
 * Constraint única (user, provider) — um usuário só tem uma conexão por provider.
 */
@Entity
@Table(
        name = "oauth_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_oauth_tokens_user_provider",
                columnNames = {"user_id", "provider"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OAuthProvider provider;

    @Column(name = "access_token_enc", nullable = false, columnDefinition = "TEXT")
    private String accessTokenEnc;

    @Column(name = "refresh_token_enc", columnDefinition = "TEXT")
    private String refreshTokenEnc;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(length = 1024)
    private String scope;

    /** E-mail da conta do provider (ex.: a conta Google que autorizou). */
    @Column(name = "provider_account_email", length = 255)
    private String providerAccountEmail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
