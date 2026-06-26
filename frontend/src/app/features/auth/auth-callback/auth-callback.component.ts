import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { SupabaseService } from '../../../services/supabase.service';

/**
 * Destino do `redirectTo` do login social. O Supabase consome o token da URL
 * (detectSessionInUrl) ao construir o client; aqui só aguardamos a sessão
 * materializar e encaminhamos para a dashboard (ou /login em caso de falha).
 */
@Component({
  selector: 'app-auth-callback',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="callback">
      @if (error()) {
        <p class="callback__error">{{ error() }}</p>
        <a routerLink="/login" class="callback__link">Voltar ao login</a>
      } @else {
        <div class="callback__spinner" aria-hidden="true"></div>
        <p>Finalizando login…</p>
      }
    </div>
  `,
  styles: [
    `
      .callback {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 1rem;
        height: 100%;
        color: var(--text-dim);
      }
      .callback__spinner {
        width: 36px;
        height: 36px;
        border: 3px solid var(--border);
        border-top-color: var(--accent);
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
      }
      .callback__error {
        color: var(--hard, #f85149);
      }
      @keyframes spin {
        to {
          transform: rotate(360deg);
        }
      }
    `,
  ],
})
export class AuthCallbackComponent implements OnInit {
  private readonly supabase = inject(SupabaseService);
  private readonly router = inject(Router);

  readonly error = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    const session = await this.supabase.ready();
    if (session) {
      // Dashboard do SaaS.
      await this.router.navigate(['/']);
    } else {
      this.error.set('Não foi possível concluir o login. Tente novamente.');
    }
  }
}
