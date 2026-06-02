package com.keepcoding.service;

import com.keepcoding.domain.OAuthToken;
import com.keepcoding.domain.enums.OAuthProvider;
import com.keepcoding.dto.StartOAuthResponse;
import com.keepcoding.security.AesGcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementa o fluxo OAuth 2.0 Authorization Code do Google.
 *
 * <ul>
 *   <li>{@link #start(String)}: gera state opaco, guarda em memória
 *       (chave = email do usuário), devolve URL de authorize.</li>
 *   <li>{@link #exchange(String, String, String)}: valida state, troca
 *       o {@code code} por tokens, busca o e-mail da conta Google e
 *       persiste via {@link OAuthTokenService}.</li>
 * </ul>
 *
 * <p>State vive 10 minutos em memória. Em produção com múltiplas
 * instâncias, mover pra Redis ou tabela dedicada.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final long STATE_TTL_MS = 10 * 60 * 1000L;

    private final OAuthTokenService tokenService;
    private final AesGcmService aes;
    private final RestClient restClient = RestClient.create();

    /** Estados pendentes: email -> (state, expiresAt). */
    private final ConcurrentHashMap<String, PendingState> pendingStates = new ConcurrentHashMap<>();

    @Value("${keepcoding.oauth.google.client-id:}")     private String clientId;
    @Value("${keepcoding.oauth.google.client-secret:}") private String clientSecret;
    @Value("${keepcoding.oauth.google.redirect-uri:http://localhost:4200/oauth/callback}")
    private String redirectUri;
    /** Scopes (separados por espaço). Default cobre Vertex AI + userinfo. */
    @Value("${keepcoding.oauth.google.scopes:openid email https://www.googleapis.com/auth/cloud-platform}")
    private String scopes;

    private record PendingState(String state, long expiresAt) {}

    /** Gera state, salva e devolve URL pro front abrir no popup. */
    public StartOAuthResponse start(String userEmail) {
        requireConfig();
        String state = randomState();
        pendingStates.put(userEmail, new PendingState(state, System.currentTimeMillis() + STATE_TTL_MS));
        evictExpired();

        String url = AUTHORIZE_URL
                + "?response_type=code"
                + "&client_id=" + urlEnc(clientId)
                + "&redirect_uri=" + urlEnc(redirectUri)
                + "&scope=" + urlEnc(scopes)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&include_granted_scopes=true"
                + "&state=" + urlEnc(state);
        return new StartOAuthResponse(url, state);
    }

    /** Valida state, troca code, persiste. */
    public void exchange(String userEmail, String code, String state) {
        requireConfig();

        PendingState pending = pendingStates.remove(userEmail);
        if (pending == null) {
            throw new IllegalArgumentException("Nenhum fluxo OAuth ativo para este usuário.");
        }
        if (pending.expiresAt() < System.currentTimeMillis()) {
            throw new IllegalArgumentException("State expirou. Refaça a conexão.");
        }
        if (!constantTimeEquals(state, pending.state())) {
            throw new IllegalArgumentException("State inválido (possível CSRF).");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<String, Object> token = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        if (token == null || token.get("access_token") == null) {
            throw new IllegalStateException("Token endpoint não devolveu access_token.");
        }

        String accessToken = (String) token.get("access_token");
        String refreshToken = (String) token.get("refresh_token");
        String scope = (String) token.get("scope");
        Number expiresIn = (Number) token.get("expires_in");
        Instant expiresAt = expiresIn == null
                ? null
                : Instant.now().plusSeconds(expiresIn.longValue());

        String providerEmail = fetchUserEmail(accessToken);
        log.info("[OAuth/Google] Conexão concluída: user={} providerAccount={}", userEmail, providerEmail);

        tokenService.save(userEmail, OAuthProvider.GOOGLE,
                accessToken, refreshToken, expiresAt, scope, providerEmail);
    }

    private String fetchUserEmail(String accessToken) {
        try {
            Map<String, Object> info = restClient.get()
                    .uri(USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
            return info == null ? null : (String) info.get("email");
        } catch (Exception e) {
            log.warn("[OAuth/Google] Falha ao buscar userinfo: {}", e.getMessage());
            return null;
        }
    }

    private void requireConfig() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException(
                    "OAuth Google não configurado. Defina GOOGLE_OAUTH_CLIENT_ID e "
                            + "GOOGLE_OAUTH_CLIENT_SECRET nas variáveis de ambiente.");
        }
    }

    private static String randomState() {
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String urlEnc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        pendingStates.entrySet().removeIf(e -> e.getValue().expiresAt() < now);
    }

    /**
     * Renova access_token usando refresh_token persistido.
     * Atualiza a linha no banco e devolve o novo access token em texto claro.
     */
    public String refresh(OAuthToken row) {
        requireConfig();
        String refreshToken = aes.decrypt(row.getRefreshTokenEnc());
        if (refreshToken == null || refreshToken.isBlank()) {
            return aes.decrypt(row.getAccessTokenEnc());
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<String, Object> token = restClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        if (token == null || token.get("access_token") == null) {
            log.warn("[OAuth/Google] Refresh falhou — usando token expirado.");
            return aes.decrypt(row.getAccessTokenEnc());
        }

        String accessToken = (String) token.get("access_token");
        String newRefresh = (String) token.get("refresh_token");
        Number expiresIn = (Number) token.get("expires_in");
        Instant expiresAt = expiresIn == null
                ? null
                : Instant.now().plusSeconds(expiresIn.longValue());

        row.setAccessTokenEnc(aes.encrypt(accessToken));
        if (newRefresh != null) {
            row.setRefreshTokenEnc(aes.encrypt(newRefresh));
        }
        row.setExpiresAt(expiresAt);
        tokenService.persist(row);

        log.info("[OAuth/Google] Token renovado para userId={}", row.getUser().getId());
        return accessToken;
    }
}
