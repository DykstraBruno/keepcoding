package com.keepcoding.dto;

import com.keepcoding.domain.enums.TierPlan;

/** Resposta de login/registro: token JWT + dados do usuário. */
public record AuthResponse(
        String token,
        Long userId,
        String username,
        String email,
        TierPlan tierPlan,
        Integer xp
) {}
