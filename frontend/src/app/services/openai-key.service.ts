import { Injectable, signal } from '@angular/core';

const STORAGE_KEY = 'kc_openai_key';

/**
 * Mantém a chave OpenAI do usuário no localStorage.
 *
 * BYOK (Bring Your Own Key): o backend nunca persiste essa chave.
 * Ela é enviada como header {@code X-OpenAI-Key} em cada chamada de IA
 * via {@code openAiKeyInterceptor}.
 */
@Injectable({ providedIn: 'root' })
export class OpenAiKeyService {
  /** Chave atual; null se o usuário ainda não cadastrou. */
  readonly key = signal<string | null>(this.readKey());

  /** true se há uma chave cadastrada. */
  hasKey(): boolean {
    return !!this.key();
  }

  /** Salva ou substitui a chave. */
  set(rawKey: string): void {
    const trimmed = rawKey.trim();
    if (!trimmed) {
      this.clear();
      return;
    }
    localStorage.setItem(STORAGE_KEY, trimmed);
    this.key.set(trimmed);
  }

  /** Remove a chave (logout do BYOK). */
  clear(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.key.set(null);
  }

  private readKey(): string | null {
    return localStorage.getItem(STORAGE_KEY);
  }
}
