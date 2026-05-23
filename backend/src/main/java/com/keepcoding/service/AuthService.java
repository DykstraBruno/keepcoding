package com.keepcoding.service;

import com.keepcoding.domain.User;
import com.keepcoding.domain.enums.TierPlan;
import com.keepcoding.dto.AuthResponse;
import com.keepcoding.dto.LoginRequest;
import com.keepcoding.dto.RegisterRequest;
import com.keepcoding.repository.UserRepository;
import com.keepcoding.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de registro e autenticação de usuários. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /** Cria a conta (senha em hash BCrypt) e já devolve um token. */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Username já em uso");
        }

        User user = userRepository.save(User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .xp(0)
                .tierPlan(TierPlan.FREE)
                .build());

        return toResponse(user);
    }

    /** Valida as credenciais e devolve um token. */
    public AuthResponse login(LoginRequest request) {
        // Lança BadCredentialsException se e-mail/senha não baterem.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        return toResponse(user);
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getTierPlan(),
                user.getXp());
    }
}
