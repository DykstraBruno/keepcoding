package com.keepcoding.domain.enums;

/** Nível de dificuldade de um problema; também define o XP concedido na primeira resolução. */
public enum Difficulty {
    EASY(8),
    MEDIUM(12),
    HARD(16);

    private final int xp;

    Difficulty(int xp) {
        this.xp = xp;
    }

    /** XP concedido na primeira vez que um problema deste nível é resolvido. */
    public int xp() {
        return xp;
    }
}
