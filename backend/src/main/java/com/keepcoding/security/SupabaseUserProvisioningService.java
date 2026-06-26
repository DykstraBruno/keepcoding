package com.keepcoding.security;

import com.keepcoding.domain.User;
import com.keepcoding.domain.enums.TierPlan;
import com.keepcoding.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Mapeia a identidade do Supabase (JWT) para um {@link User} local pelo e-mail.
 *
 * <p>Estratégia "map by email": mantém o {@code id} numérico e todo o histórico
 * local (submissões, XP, ranking). Na primeira vez que um e-mail aparece, cria
 * o usuário com username derivado dos metadados do provider social.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseUserProvisioningService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** Garante que existe um {@link User} local para o e-mail do token. */
    @Transactional
    public User ensureUser(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("JWT do Supabase sem claim 'email'.");
        }
        return userRepository.findByEmail(email)
                .orElseGet(() -> create(jwt, email));
    }

    private User create(Jwt jwt, String email) {
        Map<String, Object> meta = jwt.getClaimAsMap("user_metadata");
        String displayName = firstNonBlank(
                str(meta, "full_name"),
                str(meta, "name"),
                str(meta, "user_name"));
        String base = displayName != null ? displayName : email.split("@")[0];

        User user = User.builder()
                .email(email)
                .username(uniqueUsername(base, jwt.getSubject()))
                // Sem senha de login (auth é via Supabase); hash aleatório só p/ satisfazer NOT NULL.
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .xp(0)
                .tierPlan(TierPlan.FREE)
                .build();

        User saved = userRepository.save(user);
        log.info("[Supabase] usuário local provisionado: {} (id={})", email, saved.getId());
        return saved;
    }

    /**
     * Username único e dentro de 50 chars: base sanitizada + sufixo derivado do
     * `sub` (uuid do Supabase, único por usuário) — evita colisão na constraint.
     */
    private String uniqueUsername(String base, String sub) {
        String slug = base.trim().toLowerCase().replaceAll("[^a-z0-9_]+", "_");
        if (slug.isBlank()) {
            slug = "user";
        }
        String suffix = "_" + (sub != null ? sub.replace("-", "").substring(0, 6) : UUID.randomUUID().toString().substring(0, 6));
        int max = 50 - suffix.length();
        if (slug.length() > max) {
            slug = slug.substring(0, max);
        }
        return slug + suffix;
    }

    private static String str(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
