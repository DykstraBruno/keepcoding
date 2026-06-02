import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ConnectionService } from '../../services/connection.service';
import { OAuthProvider } from '../../models/connection.model';

/**
 * Página intermediária que o provider OAuth (Google) redireciona após o login.
 *
 * Roda DENTRO do popup. Lê {@code ?code} e {@code ?state} da URL, chama
 * {@code /api/oauth/{provider}/exchange} (com o JWT da sessão atual no
 * Authorization header — interceptor cuida) e envia
 * {@code postMessage('oauth:result', ...)} para a janela que abriu (opener).
 *
 * Em seguida tenta {@code window.close()}. Se o navegador bloquear o close,
 * mostra mensagem pedindo pro usuário fechar manualmente.
 */
@Component({
  selector: 'app-oauth-callback',
  imports: [RouterLink],
  template: `
    <div class="page">
      @if (status() === 'EXCHANGING') {
        <p>🔄 Conectando conta…</p>
      } @else if (status() === 'SUCCESS') {
        <p>✅ Conta conectada. Pode fechar esta janela.</p>
      } @else {
        <p class="error">❌ Falha: {{ error() }}</p>
        <a routerLink="/" class="link">Voltar pro app</a>
      }
    </div>
  `,
  styles: [
    `
      .page {
        min-height: 60vh;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 1rem;
        text-align: center;
        padding: 2rem;
      }
      .error {
        color: var(--hard);
        max-width: 460px;
      }
      .link {
        color: var(--accent);
      }
    `,
  ],
})
export class OAuthCallbackComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly connections = inject(ConnectionService);

  readonly status = signal<'EXCHANGING' | 'SUCCESS' | 'ERROR'>('EXCHANGING');
  readonly error = signal<string>('');

  async ngOnInit(): Promise<void> {
    const params = this.route.snapshot.queryParamMap;
    const code = params.get('code');
    const state = params.get('state');
    const errorParam = params.get('error');

    if (errorParam) {
      this.fail(errorParam);
      return;
    }
    if (!code || !state) {
      this.fail('Parâmetros code/state ausentes.');
      return;
    }

    // Provider derivado de query param ou path. Default = google.
    const provider = (params.get('provider')?.toUpperCase() ??
      'GOOGLE') as OAuthProvider;

    try {
      await this.connections.exchange(provider, code, state);
      this.status.set('SUCCESS');
      this.postToOpener({ status: 'success', provider });
      // Tenta fechar; pode falhar se popup foi aberto sem opener.
      setTimeout(() => {
        try {
          window.close();
        } catch {
          /* ignore */
        }
      }, 600);
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      this.fail(msg);
      this.postToOpener({ status: 'error', provider, error: msg });
    }
  }

  private fail(message: string): void {
    this.status.set('ERROR');
    this.error.set(message);
  }

  private postToOpener(payload: unknown): void {
    if (window.opener && window.opener !== window) {
      try {
        window.opener.postMessage(
          { type: 'oauth:result', payload },
          window.location.origin,
        );
      } catch {
        /* ignore */
      }
    }
  }
}
