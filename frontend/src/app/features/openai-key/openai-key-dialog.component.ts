import { Component, inject, signal } from '@angular/core';
import { OpenAiKeyDialogService } from '../../services/openai-key-dialog.service';
import { OpenAiKeyService } from '../../services/openai-key.service';
import { OpenRouterAuthService } from '../../services/openrouter-auth.service';

/**
 * Modal "Conectar Coach": dispara o login OAuth (PKCE) com o OpenRouter
 * e, ao voltar do callback, a chave já fica salva via {@link OpenAiKeyService}.
 *
 * Caminho longo do paste sumiu — usuário não precisa nunca ver chave
 * crua. OpenRouter é um proxy compatível com a API OpenAI; aceita
 * modelos da OpenAI, Anthropic, Google etc. com a mesma chave.
 */
@Component({
  selector: 'app-openai-key-dialog',
  imports: [],
  templateUrl: './openai-key-dialog.component.html',
  styleUrl: './openai-key-dialog.component.scss',
})
export class OpenAiKeyDialogComponent {
  readonly dialog = inject(OpenAiKeyDialogService);
  readonly keyService = inject(OpenAiKeyService);
  private readonly auth = inject(OpenRouterAuthService);

  readonly redirecting = signal(false);

  async onConnect(): Promise<void> {
    if (this.redirecting()) {
      return;
    }
    this.redirecting.set(true);
    try {
      await this.auth.startLogin();
    } catch {
      this.redirecting.set(false);
    }
  }

  onSkip(): void {
    this.dialog.resolveCancelled();
  }

  onDisconnect(): void {
    this.auth.disconnect();
  }
}
