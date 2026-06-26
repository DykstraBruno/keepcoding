package com.keepcoding.service;

import com.keepcoding.domain.OAuthToken;
import com.keepcoding.domain.User;
import com.keepcoding.domain.enums.OAuthProvider;
import com.keepcoding.dto.ConnectionDto;
import com.keepcoding.repository.OAuthTokenRepository;
import com.keepcoding.repository.UserRepository;
import com.keepcoding.security.AesGcmService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Persistência de tokens OAuth do usuário (criptografados at-rest)
 * + leitura pública (sem tokens) para a UI.
 */
@Service
public class OAuthTokenService {

    private final OAuthTokenRepository repository;
    private final UserRepository userRepository;
    private final AesGcmService aes;
    private final GoogleOAuthService googleOAuthService;

    /**
     * Construtor manual (em vez de Lombok) para conseguir aplicar
     * {@code @Lazy} no PARÂMETRO — Spring usa essa anotação só no ponto
     * de injeção, quebrando o ciclo OAuthTokenService ↔ GoogleOAuthService.
     */
    public OAuthTokenService(OAuthTokenRepository repository,
                             UserRepository userRepository,
                             AesGcmService aes,
                             @Lazy GoogleOAuthService googleOAuthService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.aes = aes;
        this.googleOAuthService = googleOAuthService;
    }

    /**
     * Upsert: substitui o token existente do par (user, provider) ou cria.
     * accessToken e refreshToken são criptografados antes de gravar.
     */
    @Transactional
    public void save(String userEmail,
                     OAuthProvider provider,
                     String accessToken,
                     String refreshToken,
                     Instant expiresAt,
                     String scope,
                     String providerAccountEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário: " + userEmail));

        OAuthToken row = repository.findByUserIdAndProvider(user.getId(), provider)
                .orElseGet(() -> OAuthToken.builder().user(user).provider(provider).build());

        row.setAccessTokenEnc(aes.encrypt(accessToken));
        if (refreshToken != null) {
            row.setRefreshTokenEnc(aes.encrypt(refreshToken));
        }
        row.setExpiresAt(expiresAt);
        row.setScope(scope);
        row.setProviderAccountEmail(providerAccountEmail);
        repository.save(row);
    }

    /** Lista de conexões do usuário, sem expor tokens. */
    @Transactional
    public List<ConnectionDto> listConnections(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário: " + userEmail));
        return repository.findByUserId(user.getId()).stream()
                .map(t -> new ConnectionDto(
                        t.getProvider(),
                        t.getProviderAccountEmail(),
                        t.getCreatedAt(),
                        t.getExpiresAt(),
                        t.getScope()))
                .toList();
    }

    @Transactional
    public void disconnect(String userEmail, OAuthProvider provider) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário: " + userEmail));
        repository.deleteByUserIdAndProvider(user.getId(), provider);
    }

    /**
     * Devolve access token válido do provider (renova via refresh_token se expirado).
     * Vazio se o usuário não conectou esse provider.
     */
    @Transactional
    public Optional<String> getValidAccessToken(String userEmail, OAuthProvider provider) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário: " + userEmail));

        return repository.findByUserIdAndProvider(user.getId(), provider)
                .map(token -> resolveAccessToken(token, provider));
    }

    /** true se o usuário tem ao menos uma conexão OAuth ativa. */
    @Transactional
    public boolean hasAnyConnection(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário: " + userEmail));
        return !repository.findByUserId(user.getId()).isEmpty();
    }

    private String resolveAccessToken(OAuthToken token, OAuthProvider provider) {
        Instant expiresAt = token.getExpiresAt();
        boolean expired = expiresAt != null && expiresAt.isBefore(Instant.now().plusSeconds(60));
        if (!expired) {
            return aes.decrypt(token.getAccessTokenEnc());
        }
        if (token.getRefreshTokenEnc() == null) {
            return aes.decrypt(token.getAccessTokenEnc());
        }
        if (provider != OAuthProvider.GOOGLE) {
            return aes.decrypt(token.getAccessTokenEnc());
        }
        return googleOAuthService.refresh(token);
    }

    @Transactional
    public void persist(OAuthToken row) {
        repository.save(row);
    }
}
