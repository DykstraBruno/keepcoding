package com.keepcoding.controller;

import com.keepcoding.domain.enums.OAuthProvider;
import com.keepcoding.dto.ConnectionDto;
import com.keepcoding.dto.OAuthExchangeRequest;
import com.keepcoding.dto.StartOAuthResponse;
import com.keepcoding.service.GoogleOAuthService;
import com.keepcoding.service.OAuthTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints do fluxo OAuth de providers externos (Google etc.).
 *
 * Fluxo no front:
 *  1) POST /api/oauth/google/start          → { authorizeUrl, state }
 *  2) window.open(authorizeUrl)             → popup do Google
 *  3) Google redireciona pra /oauth/callback no front (com ?code+state)
 *  4) Front faz POST /api/oauth/google/exchange { code, state }
 *  5) Front fecha popup via postMessage → opener atualiza UI
 */
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final GoogleOAuthService googleOAuthService;
    private final OAuthTokenService tokenService;

    @PostMapping("/google/start")
    public StartOAuthResponse startGoogle(@AuthenticationPrincipal UserDetails principal) {
        return googleOAuthService.start(principal.getUsername());
    }

    @PostMapping("/google/exchange")
    public ResponseEntity<Void> exchangeGoogle(
            @Valid @RequestBody OAuthExchangeRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        googleOAuthService.exchange(principal.getUsername(), request.code(), request.state());
        return ResponseEntity.noContent().build();
    }

    /** Lista as conexões do usuário (sem tokens). */
    @GetMapping("/connections")
    public List<ConnectionDto> connections(@AuthenticationPrincipal UserDetails principal) {
        return tokenService.listConnections(principal.getUsername());
    }

    @DeleteMapping("/connections/{provider}")
    public ResponseEntity<Void> disconnect(@PathVariable OAuthProvider provider,
                                           @AuthenticationPrincipal UserDetails principal) {
        tokenService.disconnect(principal.getUsername(), provider);
        return ResponseEntity.noContent().build();
    }
}
