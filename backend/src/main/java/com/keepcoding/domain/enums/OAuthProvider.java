package com.keepcoding.domain.enums;

/** Providers de IA que aceitam OAuth público pra acesso à API. */
public enum OAuthProvider {
    /** Google (Vertex AI Gemini). Único provider de IA com OAuth público real. */
    GOOGLE,
    /** Anthropic — placeholder. Sem OAuth público ainda. */
    ANTHROPIC,
    /** OpenAI — placeholder. Sem OAuth público para acesso à API. */
    OPENAI
}
