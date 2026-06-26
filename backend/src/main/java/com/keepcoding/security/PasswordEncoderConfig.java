package com.keepcoding.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bean isolado para o {@link PasswordEncoder}.
 *
 * <p>Vive fora de {@code SecurityConfig} pra quebrar o ciclo:
 * SecurityConfig → SupabaseJwtAuthenticationConverter → SupabaseUserProvisioningService
 * → PasswordEncoder (antes definido no próprio SecurityConfig).</p>
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
