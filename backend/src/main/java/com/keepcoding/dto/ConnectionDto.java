package com.keepcoding.dto;

import com.keepcoding.domain.enums.OAuthProvider;

import java.time.Instant;

/**
 * Resumo de uma conexão OAuth ativa do usuário.
 * NÃO inclui tokens — só metadata segura pra exibir na UI.
 */
public record ConnectionDto(
        OAuthProvider provider,
        String providerAccountEmail,
        Instant connectedAt,
        Instant expiresAt,
        String scope
) {}
