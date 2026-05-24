package com.keepcoding.domain.enums;

/** Tipos de evento publicados ao topic da partida durante o duelo. */
public enum MatchEventType {
    /** Partida formada — clientes devem renderizar a tela do duelo. */
    MATCH_FOUND,
    /** Algum jogador enviou uma submissão (status no payload). */
    SUBMISSION,
    /** Um jogador acertou primeiro e venceu. */
    WIN,
    /** Forfeit: violação de anti-cheat ou desistência. */
    FORFEIT,
    /** Resumo final disparado ao final da partida. */
    MATCH_ENDED
}
