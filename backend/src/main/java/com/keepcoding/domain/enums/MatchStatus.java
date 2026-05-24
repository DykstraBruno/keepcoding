package com.keepcoding.domain.enums;

/** Estado de uma partida (duelo) em tempo real. */
public enum MatchStatus {
    /** Em disputa. */
    ACTIVE,
    /** Alguém venceu (primeiro ACCEPTED). */
    COMPLETED,
    /** Encerrada por forfeit (violação de anti-cheat ou desistência). */
    ABANDONED
}
