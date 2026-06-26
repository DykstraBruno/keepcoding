import { Injectable, computed, signal } from '@angular/core';
import { AiKeyConfig, AiKeyProvider, isValidAiKey } from '../models/ai-key.model';

const STORAGE_KEY = 'keepcoding.ai-key.v1';

/**
 * BYOK store: chave de IA do usuário persistida em localStorage.
 *
 * Estado é um signal — UI reativa automaticamente quando muda. A chave
 * é enviada por header em cada request via {@code aiKeyInterceptor};
 * NUNCA é gravada no backend.
 *
 * Trade-off conhecido: localStorage é vulnerável a XSS — toda a aplicação
 * deve manter CSP estrita e sanitização rigorosa de entradas de terceiros.
 */
@Injectable({ providedIn: 'root' })
export class AiKeyService {
  private readonly _config = signal<AiKeyConfig | null>(this.load());

  readonly config = this._config.asReadonly();
  readonly isConfigured = computed(() => isValidAiKey(this._config()));
  readonly provider = computed<AiKeyProvider | null>(() => this._config()?.provider ?? null);

  save(cfg: AiKeyConfig): void {
    const provider = cfg.provider;
    if (!isValidAiKey(cfg)) {
      throw new Error('Formato de chave inválido para ' + provider);
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(cfg));
    this._config.set(cfg);
  }

  clear(): void {
    localStorage.removeItem(STORAGE_KEY);
    this._config.set(null);
  }

  private load(): AiKeyConfig | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw) as AiKeyConfig;
      return isValidAiKey(parsed) ? parsed : null;
    } catch {
      return null;
    }
  }
}
