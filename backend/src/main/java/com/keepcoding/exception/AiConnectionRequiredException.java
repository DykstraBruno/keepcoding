package com.keepcoding.exception;

/** Usuário tentou usar Coach/Entrevistador sem conta de IA conectada via OAuth. */
public class AiConnectionRequiredException extends RuntimeException {

    public AiConnectionRequiredException() {
        super("Conecte sua conta de IA (OAuth) antes de usar o Coach ou o Entrevistador.");
    }
}
