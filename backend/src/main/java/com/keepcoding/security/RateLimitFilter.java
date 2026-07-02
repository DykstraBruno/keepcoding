package com.keepcoding.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Rate limit por IP + categoria de endpoint (token bucket, Redis-backed).
 *
 * <p>3 categorias:
 * <ul>
 *   <li><b>AI</b> — {@code /api/submissions}, {@code /api/interviews},
 *       {@code /api/architecture}: caras ao provider externo.</li>
 *   <li><b>AUTH_LIKE</b> — {@code /api/oauth/**}: sensível a força bruta.</li>
 *   <li><b>GENERAL</b> — resto de {@code /api/**}.</li>
 * </ul>
 *
 * <p>Sobre 429 devolve {@code Retry-After} (segundos) + {@code X-RateLimit-*}
 * headers pra o cliente saber quando tentar de novo. Fora de {@code /api/**}
 * (assets estáticos, healthcheck) o filtro é no-op.</p>
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties props;

    public RateLimitFilter(ProxyManager<String> proxyManager, RateLimitProperties props) {
        this.proxyManager = proxyManager;
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // fora de /api/** nao ha o que limitar (WS handshake tem categoria propria abaixo)
        String path = request.getRequestURI();
        return !props.enabled()
                || (!path.startsWith("/api/") && !path.startsWith("/ws"));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        Category category = categorize(request.getRequestURI());
        int limitPerMinute = limitFor(category);
        String key = "rl:" + category.name().toLowerCase() + ":" + clientIp(request);

        BucketProxy bucket = proxyManager.builder()
                .build(key, configOf(limitPerMinute));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader("X-RateLimit-Limit", Integer.toString(limitPerMinute));
        response.setHeader("X-RateLimit-Remaining", Long.toString(Math.max(0, probe.getRemainingTokens())));

        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }

        long waitSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(waitSeconds));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"error\":\"Muitas requisicoes. Aguarde " + waitSeconds
                        + "s antes de tentar de novo.\",\"category\":\"" + category.name()
                        + "\",\"retryAfterSeconds\":" + waitSeconds + "}");
        log.warn("[rate-limit] 429 ip={} category={} path={}",
                clientIp(request), category, request.getRequestURI());
    }

    // ------------------------------------------------------------ helpers

    private Supplier<BucketConfiguration> configOf(int limitPerMinute) {
        return () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limitPerMinute)
                        .refillIntervally(limitPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private int limitFor(Category c) {
        return switch (c) {
            case AI -> props.ai();
            case AUTH_LIKE -> props.authLike();
            case GENERAL -> props.general();
        };
    }

    private Category categorize(String path) {
        if (path.startsWith("/api/submissions")
                || path.startsWith("/api/interviews")
                || path.startsWith("/api/architecture")) {
            return Category.AI;
        }
        if (path.startsWith("/api/oauth")) {
            return Category.AUTH_LIKE;
        }
        return Category.GENERAL;
    }

    /**
     * IP do cliente. Confia em {@code X-Forwarded-For} quando presente (deploy
     * atrás de proxy reverso); caso contrário {@code getRemoteAddr()}.
     */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    private enum Category { AI, AUTH_LIKE, GENERAL }
}
