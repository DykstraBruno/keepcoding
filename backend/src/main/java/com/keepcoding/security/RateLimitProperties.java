package com.keepcoding.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites por minuto por categoria de endpoint.
 *
 * <p>Categorias:
 * <ul>
 *   <li>{@code ai} — endpoints que chamam IA (submissions, interviews,
 *       architecture). São os mais caros pro provider externo.</li>
 *   <li>{@code authLike} — endpoints de conexão de conta / OAuth (start,
 *       exchange). Sensíveis a força-bruta.</li>
 *   <li>{@code general} — todo o resto (listagens, ranking etc).</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "keepcoding.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int ai,
        int authLike,
        int general
) {
    public RateLimitProperties {
        if (ai <= 0) ai = 20;
        if (authLike <= 0) authLike = 10;
        if (general <= 0) general = 60;
    }
}
