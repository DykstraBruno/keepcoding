import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ConnectionService } from '../../services/connection.service';
import { OAuthPopupService } from '../../services/oauth-popup.service';
import { AiKeyService } from '../../services/ai-key.service';
import { ConnectionDialogService } from './connection-dialog.service';
import { OAuthProvider } from '../../models/connection.model';
import {
  AI_PROVIDER_HINTS,
  AI_PROVIDER_LABELS,
  AiKeyProvider,
  isValidAiKey,
  maskApiKey,
} from '../../models/ai-key.model';

interface ProviderCard {
  id: OAuthProvider;
  label: string;
  description: string;
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
    unavailableReason: 'Use BYOK (chave própria) abaixo ou Google OAuth acima.',
  },
];

const BYOK_PROVIDERS: readonly AiKeyProvider[] = ['OPENAI', 'GOOGLE'];

/**
 * Modal "Conectar IA". Duas vias coexistem:
 *  - OAuth (Google): autorização sem chave API, token criptografado no servidor.
 *  - BYOK (Bring Your Own Key): chave OpenAI ou Gemini guardada no browser do
 *    usuário, enviada por header em cada request. Zero custo de IA pro servidor.
 */
@Component({
  selector: 'app-connection-dialog',
  imports: [FormsModule],
  templateUrl: './connection-dialog.component.html',
  styleUrl: './connection-dialog.component.scss',
})
export class ConnectionDialogComponent {
  readonly dialog = inject(ConnectionDialogService);
  readonly connections = inject(ConnectionService);
  readonly aiKey = inject(AiKeyService);
  private readonly popup = inject(OAuthPopupService);

  readonly providers = PROVIDERS;
  readonly byokProviders = BYOK_PROVIDERS;
  readonly providerLabels = AI_PROVIDER_LABELS;
  readonly providerHints = AI_PROVIDER_HINTS;

  readonly busy = signal<OAuthProvider | null>(null);
  readonly lastError = signal<string | null>(null);

  readonly byokProvider = signal<AiKeyProvider>('OPENAI');
  readonly byokKey = signal('');
  readonly byokRevealed = signal(false);
  readonly byokSaving = signal(false);
  readonly byokError = signal<string | null>(null);

  readonly anyConnected = computed(
    () => this.connections.connections().length > 0 || this.aiKey.isConfigured(),
  );

  readonly currentByokHint = computed(() => this.providerHints[this.byokProvider()]);

  readonly byokValid = computed(() =>
    isValidAiKey({ provider: this.byokProvider(), apiKey: this.byokKey() }),
  );

  /** Visão segura da chave salva para mostrar no card "configurada". */
  readonly savedKeyMasked = computed(() => {
    const cfg = this.aiKey.config();
    return cfg ? maskApiKey(cfg.apiKey) : null;
  });

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

  saveByok(): void {
    this.byokError.set(null);
    try {
      this.byokSaving.set(true);
      this.aiKey.save({
        provider: this.byokProvider(),
        apiKey: this.byokKey().trim(),
      });
      this.byokKey.set('');
      this.byokRevealed.set(false);
    } catch (e) {
      this.byokError.set(e instanceof Error ? e.message : String(e));
    } finally {
      this.byokSaving.set(false);
    }
  }

  clearByok(): void {
    this.aiKey.clear();
    this.byokKey.set('');
    this.byokError.set(null);
  }

  close(): void {
    this.dialog.close();
  }
}
