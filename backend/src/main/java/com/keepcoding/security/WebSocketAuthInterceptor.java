package com.keepcoding.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Valida o JWT do Supabase enviado no header Authorization do frame STOMP
 * CONNECT e amarra o Principal (e-mail) à sessão WebSocket — usado por
 * {@code SimpMessagingTemplate.convertAndSendToUser(email, ...)}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final SupabaseUserProvisioningService provisioning;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            log.debug("STOMP CONNECT sem Bearer token — recusando.");
            return null; // interrompe a entrega → conexão cai
        }
        String token = header.substring(7);
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String email = provisioning.ensureUser(jwt).getEmail();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            accessor.setUser(auth);
            return message;
        } catch (JwtException e) {
            log.debug("STOMP CONNECT com token inválido: {}", e.getMessage());
            return null;
        }
    }
}
