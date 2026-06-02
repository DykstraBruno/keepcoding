package com.keepcoding.service;

import com.keepcoding.domain.enums.OAuthProvider;
import com.keepcoding.exception.AiConnectionRequiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Constrói um {@link ChatClient} por chamada usando o token OAuth do usuário
 * (Google → Gemini via camada compatível OpenAI).
 *
 * <p>Fluxo de produção: usuário autoriza via OAuth popup; o backend usa o
 * access token criptografado no banco — nunca expõe chave API no browser.</p>
 *
 * <p>Fallback de dev: se {@code OPENAI_API_KEY} global estiver definida e o
 * usuário não tiver OAuth, usa OpenAI direto (útil sem credenciais Google).</p>
 */
@Slf4j
@Component
public class UserScopedChatClientFactory {

    private static final String PLACEHOLDER = "changeme";
    private static final String GEMINI_OPENAI_BASE =
            "https://generativelanguage.googleapis.com/v1beta/openai/";

    private final OAuthTokenService oauthTokenService;
    private final String defaultApiKey;
    private final String openAiModel;
    private final String geminiModel;
    private final Double defaultTemperature;
    private final boolean requireOAuth;

    public UserScopedChatClientFactory(
            OAuthTokenService oauthTokenService,
            @Value("${spring.ai.openai.api-key:}") String defaultApiKey,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String openAiModel,
            @Value("${keepcoding.ai.gemini.model:gemini-2.0-flash}") String geminiModel,
            @Value("${spring.ai.openai.chat.options.temperature:0.4}") Double defaultTemperature,
            @Value("${keepcoding.ai.require-oauth:true}") boolean requireOAuth) {
        this.oauthTokenService = oauthTokenService;
        this.defaultApiKey = defaultApiKey;
        this.openAiModel = openAiModel;
        this.geminiModel = geminiModel;
        this.defaultTemperature = defaultTemperature;
        this.requireOAuth = requireOAuth;
    }

    /**
     * ChatClient do usuário autenticado. Prioriza token OAuth (Google/Gemini).
     * Lança {@link AiConnectionRequiredException} se OAuth é obrigatório e
     * o usuário não conectou conta de IA.
     */
    public ChatClient forUser(String userEmail) {
        return oauthTokenService.getValidAccessToken(userEmail, OAuthProvider.GOOGLE)
                .map(this::buildGeminiClient)
                .orElseGet(() -> {
                    ChatClient devFallback = buildOpenAiClient(pickGlobalKey());
                    if (devFallback != null) {
                        log.debug("[AI] Usando OPENAI_API_KEY global (dev) para {}", userEmail);
                        return devFallback;
                    }
                    if (requireOAuth) {
                        throw new AiConnectionRequiredException();
                    }
                    return null;
                });
    }

    private ChatClient buildGeminiClient(String accessToken) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(GEMINI_OPENAI_BASE)
                .apiKey(accessToken)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(geminiModel)
                .temperature(defaultTemperature)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
        return ChatClient.builder(model).build();
    }

    private ChatClient buildOpenAiClient(String apiKey) {
        if (apiKey == null) {
            return null;
        }
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(openAiModel)
                .temperature(defaultTemperature)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
        return ChatClient.builder(model).build();
    }

    private String pickGlobalKey() {
        if (defaultApiKey != null && !defaultApiKey.isBlank() && !PLACEHOLDER.equals(defaultApiKey)) {
            return defaultApiKey.trim();
        }
        return null;
    }
}
