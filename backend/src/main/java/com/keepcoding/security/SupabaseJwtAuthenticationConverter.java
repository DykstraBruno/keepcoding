package com.keepcoding.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converte o JWT validado do Supabase numa {@link org.springframework.security.core.Authentication}
 * cujo principal é um {@link UserDetails} com username = e-mail.
 *
 * <p>Isso preserva o contrato dos controllers existentes
 * ({@code @AuthenticationPrincipal UserDetails} + {@code getUsername()} = e-mail)
 * sem precisar tocá-los. Como efeito colateral, garante o usuário local (map by email).</p>
 */
@Component
@RequiredArgsConstructor
public class SupabaseJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final SupabaseUserProvisioningService provisioning;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // Garante o User local e usa o e-mail canônico do banco como principal.
        String email = provisioning.ensureUser(jwt).getEmail();

        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_USER"));

        UserDetails principal = User.builder()
                .username(email)
                .password("") // não há senha local; auth é via Supabase
                .authorities(authorities)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }
}
