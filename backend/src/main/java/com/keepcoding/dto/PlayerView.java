package com.keepcoding.dto;

import com.keepcoding.domain.User;

/** Representação enxuta de um jogador para frontends. */
public record PlayerView(Long id, String username) {

    public static PlayerView from(User user) {
        return new PlayerView(user.getId(), user.getUsername());
    }
}
