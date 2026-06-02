import { Component, computed, inject, signal } from '@angular/core';
import { ConnectionService } from '../../services/connection.service';
import { OAuthPopupService } from '../../services/oauth-popup.service';
import { ConnectionDialogService } from './connection-dialog.service';
import { OAuthProvider } from '../../models/connection.model';

interface ProviderCard {
  id: OAuthProvider;
  label: string;
  description: string;
  /** false desabilita o botão; explica o motivo. */
  available: boolean;
  unavailableReason?: string;
}

const PROVIDERS: readonly ProviderCard[] = [
  {
    id: 'GOOGLE',
    label: 'Google (IA)',
    description:
      'Autorize o KeepCoding a usar IA em seu nome. Você será redirecionado para login seguro — sem chaves API.',
    available: true,
  },
  {
    id: 'ANTHROPIC',
    label: 'Anthropic (Claude)',
    description: 'Em breve — aguardando OAuth público da plataforma.',
    available: false,
    unavailableReason: 'Provider ainda indisponível para conexão OAuth.',
  },
  {
    id: 'OPENAI',
    label: 'OpenAI (ChatGPT)',
    description: 'Em breve — OpenAI ainda não oferece OAuth para apps web.',
    available: false,
    unavailableReason: 'Use Google (IA) por enquanto — mesmo fluxo de autorização OAuth.',
  },
];

/**
 * Modal "Conectar Conta". Lista os providers de IA suportados, abre
 * o popup OAuth quando o usuário clica no card disponível e mostra
 * status atual (conectado / desconectar).
 */
@Component({
  selector: 'app-connection-dialog',
  imports: [],
  templateUrl: './connection-dialog.component.html',
  styleUrl: './connection-dialog.component.scss',
})
export class ConnectionDialogComponent {
  readonly dialog = inject(ConnectionDialogService);
  readonly connections = inject(ConnectionService);
  private readonly popup = inject(OAuthPopupService);

  readonly providers = PROVIDERS;
  readonly busy = signal<OAuthProvider | null>(null);
  readonly lastError = signal<string | null>(null);

  /** true se qualquer provider já está conectado. */
  readonly anyConnected = computed(() => this.connections.connections().length > 0);

  async connect(provider: OAuthProvider): Promise<void> {
    if (!PROVIDERS.find((p) => p.id === provider)?.available) {
      return;
    }
    this.busy.set(provider);
    this.lastError.set(null);
    try {
      const result = await this.popup.openProviderLogin(provider);
      if (result.status === 'error') {
        this.lastError.set(result.error ?? 'Erro durante autorização.');
      }
    } catch (e) {
      this.lastError.set(e instanceof Error ? e.message : String(e));
    } finally {
      this.busy.set(null);
    }
  }

  async disconnect(provider: OAuthProvider): Promise<void> {
    this.busy.set(provider);
    try {
      await this.connections.disconnect(provider);
    } finally {
      this.busy.set(null);
    }
  }

  close(): void {
    this.dialog.close();
  }
}
