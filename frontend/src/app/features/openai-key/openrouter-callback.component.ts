import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OpenRouterAuthService } from '../../services/openrouter-auth.service';

/**
 * Página intermediária que recebe o redirect do OpenRouter
 * em {@code /auth/openrouter/callback?code=...}, troca o code pela chave
 * via {@link OpenRouterAuthService#handleCallback} e volta pra home.
 */
@Component({
  selector: 'app-openrouter-callback',
  imports: [],
  template: `
    <div class="page">
      @if (status() === 'EXCHANGING') {
        <p>Conectando ao Coach…</p>
      } @else if (status() === 'ERROR') {
        <p class="error">Falha na conexão com OpenRouter: {{ error() }}</p>
        <a routerLink="/" class="link">Voltar</a>
      }
    </div>
  `,
  styles: [
    `
      .page {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        min-height: 60vh;
        gap: 1rem;
      }
      .error {
        color: var(--hard);
        text-align: center;
        max-width: 480px;
      }
      .link {
        color: var(--accent);
      }
    `,
  ],
})
export class OpenRouterCallbackComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(OpenRouterAuthService);

  readonly status = signal<'EXCHANGING' | 'ERROR'>('EXCHANGING');
  readonly error = signal<string>('');

  async ngOnInit(): Promise<void> {
    const code = this.route.snapshot.queryParamMap.get('code');
    if (!code) {
      this.status.set('ERROR');
      this.error.set('Código de autorização ausente na URL.');
      return;
    }
    try {
      await this.auth.handleCallback(code);
      this.router.navigate(['/']);
    } catch (e) {
      this.status.set('ERROR');
      this.error.set(e instanceof Error ? e.message : String(e));
    }
  }
}
