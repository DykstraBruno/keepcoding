package com.keepcoding.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Constrói um {@link ChatClient} por chamada usando a chave OpenAI fornecida
 * pelo usuário (BYOK — Bring Your Own Key).
 *
 * <p>Estratégia:</p>
 * <ul>
 *   <li>Se a chave do usuário for vazia, usa a chave global de configuração
 *       (útil em dev). Se também estiver vazia ou for o placeholder
 *       {@code changeme}, devolve {@code null} — sinal pro AI service usar
 *       o feedback de fallback.</li>
 *   <li>O modelo a ser usado vem da configuração
 *       {@code spring.ai.openai.chat.options.model} (padrão {@code gpt-4o-mini}).</li>
 *   <li>Cada call cria seus próprios objetos OpenAiApi/ChatModel/ChatClient.
 *       Custo desprezível e mantém isolamento entre usuários.</li>
 * </ul>
 */
@Component
public class UserScopedChatClientFactory {

    private static final String PLACEHOLDER = "changeme";

    private final String baseUrl;
    private final String defaultApiKey;
    private final String defaultModel;
    private final Double defaultTemperature;

    public UserScopedChatClientFactory(
            @Value("${spring.ai.openai.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${spring.ai.openai.api-key:}") String defaultApiKey,
            @Value("${spring.ai.openai.chat.options.model:openai/gpt-4o-mini}") String defaultModel,
            @Value("${spring.ai.openai.chat.options.temperature:0.4}") Double defaultTemperature) {
        this.baseUrl = baseUrl;
        this.defaultApiKey = defaultApiKey;
        this.defaultModel = defaultModel;
        this.defaultTemperature = defaultTemperature;
    }

    /**
     * Devolve um {@link ChatClient} pronto pra uso ou {@code null} se nem o
     * usuário nem a configuração têm uma chave válida.
     */
    public ChatClient forApiKey(String userApiKey) {
        String key = pickKey(userApiKey);
        if (key == null) {
            return null;
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(key)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(defaultModel)
                .temperature(defaultTemperature)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
        return ChatClient.builder(model).build();
    }

    private String pickKey(String userApiKey) {
        if (userApiKey != null && !userApiKey.isBlank() && !PLACEHOLDER.equals(userApiKey)) {
            return userApiKey.trim();
        }
        if (defaultApiKey != null && !defaultApiKey.isBlank() && !PLACEHOLDER.equals(defaultApiKey)) {
            return defaultApiKey.trim();
        }
        return null;
    }
}
