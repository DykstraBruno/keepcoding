package com.keepcoding.security;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

/**
 * Acesso request-scoped à chave BYOK que o usuário envia em cada chamada.
 *
 * <p>Não persiste em DB e não loga em texto puro. O {@link ByokKeyFilter}
 * extrai dos headers e publica aqui; a {@code UserScopedChatClientFactory}
 * lê para construir um ChatClient com a chave fornecida pelo browser.</p>
 */
public final class ByokContext {

    private static final String ATTR_PROVIDER = "keepcoding.byok.provider";
    private static final String ATTR_KEY = "keepcoding.byok.key";

    private ByokContext() {}

    public record Key(String provider, String apiKey) {}

    public static Optional<Key> current() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return Optional.empty();
        }
        Object provider = attrs.getAttribute(ATTR_PROVIDER, RequestAttributes.SCOPE_REQUEST);
        Object key = attrs.getAttribute(ATTR_KEY, RequestAttributes.SCOPE_REQUEST);
        if (provider == null || key == null) {
            return Optional.empty();
        }
        return Optional.of(new Key(provider.toString(), key.toString()));
    }

    static void put(String provider, String apiKey) {
        RequestAttributes attrs = RequestContextHolder.currentRequestAttributes();
        attrs.setAttribute(ATTR_PROVIDER, provider, RequestAttributes.SCOPE_REQUEST);
        attrs.setAttribute(ATTR_KEY, apiKey, RequestAttributes.SCOPE_REQUEST);
    }
}
