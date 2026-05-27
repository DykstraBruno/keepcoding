import { Injectable, inject, signal } from '@angular/core';
import { OpenAiKeyService } from './openai-key.service';

/**
 * Controla a visibilidade do modal de cadastro da chave OpenAI.
 *
 * Componentes que precisam da chave antes de uma ação de IA chamam
 * {@link requestKey}. O componente do modal (renderizado no AppComponent)
 * observa o signal {@link visible} e, ao salvar/cancelar, resolve a promise.
 */
@Injectable({ providedIn: 'root' })
export class OpenAiKeyDialogService {
  private readonly keyService = inject(OpenAiKeyService);

  readonly visible = signal(false);

  private pendingResolve: ((key: string | null) => void) | null = null;

  /**
   * Garante que existe uma chave. Se já houver, devolve direto.
   * Caso contrário, abre o modal e aguarda o usuário salvar ou cancelar.
   *
   * @returns a chave (string) se foi cadastrada/já existia,
   *          ou {@code null} se o usuário cancelou.
   */
  requestKey(): Promise<string | null> {
    const existing = this.keyService.key();
    if (existing) {
      return Promise.resolve(existing);
    }
    return new Promise<string | null>((resolve) => {
      this.pendingResolve = resolve;
      this.visible.set(true);
    });
  }

  /** Abre o modal sem promise — usado pelo link "Atualizar chave" no header. */
  open(): void {
    this.visible.set(true);
  }

  /** Chamado pelo componente do modal quando o usuário salva. */
  resolveSaved(rawKey: string): void {
    this.keyService.set(rawKey);
    this.visible.set(false);
    this.pendingResolve?.(this.keyService.key());
    this.pendingResolve = null;
  }

  /** Chamado quando o usuário fecha sem salvar. */
  resolveCancelled(): void {
    this.visible.set(false);
    this.pendingResolve?.(null);
    this.pendingResolve = null;
  }
}
