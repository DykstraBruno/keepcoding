package com.keepcoding.dto;

/** Resposta de POST /api/oauth/{provider}/start. */
public record StartOAuthResponse(
        /** URL pra qual o frontend deve abrir o popup. */
        String authorizeUrl,
        /** State opaco — frontend devolve no exchange (defesa CSRF). */
        String state
) {}
