package com.keepcoding.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * BYOK: lê {@code X-AI-Provider} + {@code X-AI-Key} dos headers, valida o
 * formato esperado por provider e publica em {@link ByokContext} para uso
 * request-scoped pela factory de ChatClient.
 *
 * <p>Não persiste a chave. Não loga em texto puro (apenas os 4 últimos chars).</p>
 *
 * <p>Registrado em {@code SecurityConfig} via {@code addFilterBefore} — não
 * usa {@code @Component} para evitar auto-registro duplicado pelo Spring Boot.</p>
 */
@Slf4j
public class ByokKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_PROVIDER = "X-AI-Provider";
    public static final String HEADER_KEY = "X-AI-Key";

    /** Regex de validação por provider — formato canônico do emissor da chave. */
    private static final Map<String, Pattern> FORMATS = Map.of(
            "OPENAI", Pattern.compile("^sk-[A-Za-z0-9_-]{20,200}$"),
            "GOOGLE", Pattern.compile("^AIza[A-Za-z0-9_-]{30,80}$")
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String provider = request.getHeader(HEADER_PROVIDER);
        String rawKey = request.getHeader(HEADER_KEY);

        if (provider != null && rawKey != null
                && !provider.isBlank() && !rawKey.isBlank()) {

            String normalized = provider.trim().toUpperCase();
            String key = rawKey.trim();
            Pattern fmt = FORMATS.get(normalized);

            if (fmt == null) {
                writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Provider BYOK desconhecido: " + normalized);
                return;
            }
            if (!fmt.matcher(key).matches()) {
                writeJsonError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Formato de chave invalido para " + normalized);
                return;
            }

            ByokContext.put(normalized, key);
            log.debug("[BYOK] aceita provider={} ...{}", normalized, lastFour(key));
        }

        chain.doFilter(request, response);
    }

    private static String lastFour(String s) {
        return s.length() <= 4 ? "****" : s.substring(s.length() - 4);
    }

    private static void writeJsonError(HttpServletResponse resp, int status, String msg)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"error\":\"" + msg.replace("\"", "'") + "\"}");
    }
}
