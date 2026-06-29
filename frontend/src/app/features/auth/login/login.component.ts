import { Component, computed, inject, signal } from '@angular/core';
import { Provider } from '@supabase/supabase-js';
import { AuthService } from '../../../services/auth.service';
import { TurnstileComponent } from '../turnstile.component';

interface SocialButton {
  provider: Provider;
  label: string;
  icon: string;
}

/** Tela de login social (Supabase Auth): Google, Apple e GitHub. */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [TurnstileComponent],
  template: `
    <div class="login">
      <div class="login__card">
        <div class="login__brand">
          <span class="login__logo">&lt;/&gt;</span>
          <h1 class="login__title">KeepCoding</h1>
        </div>
        <p class="login__subtitle">Entre para continuar praticando.</p>

        @if (error()) {
          <p class="login__error" role="alert">{{ error() }}</p>
        }

        <app-turnstile (tokenChange)="onCaptchaToken($event)" />

        @if (!captchaReady() && !error()) {
          <p class="login__hint">Resolva a verificação para continuar.</p>
        }

        <div class="login__providers">
          @for (btn of buttons; track btn.provider) {
            <button
              type="button"
              class="login__btn"
              [class.login__btn--loading]="loading() === btn.provider"
              [disabled]="loading() !== null || !captchaReady()"
              (click)="signIn(btn.provider)">
              <span class="login__btn-icon" [innerHTML]="btn.icon"></span>
              <span>Continuar com {{ btn.label }}</span>
            </button>
          }
        </div>

        <p class="login__legal">
          Ao entrar você concorda com os Termos e a Política de Privacidade.
        </p>
      </div>
    </div>
  `,
  styles: [
    `
      .login {
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 100%;
        padding: 1.5rem;
      }
      .login__card {
        width: 100%;
        max-width: 360px;
        padding: 2rem 1.75rem;
        background: var(--panel);
        border: 1px solid var(--border);
        border-radius: 14px;
        box-shadow: 0 12px 40px rgba(0, 0, 0, 0.35);
      }
      .login__brand {
        display: flex;
        align-items: center;
        gap: 0.6rem;
        margin-bottom: 0.4rem;
      }
      .login__logo {
        color: var(--accent);
        font-weight: 800;
        font-family: monospace;
        font-size: 1.3rem;
      }
      .login__title {
        margin: 0;
        font-size: 1.35rem;
        font-weight: 700;
      }
      .login__subtitle {
        margin: 0 0 1.5rem;
        color: var(--text-dim);
        font-size: 0.9rem;
      }
      .login__providers {
        display: flex;
        flex-direction: column;
        gap: 0.7rem;
      }
      .login__btn {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.6rem;
        width: 100%;
        padding: 0.7rem 1rem;
        font-size: 0.92rem;
        font-weight: 600;
        color: var(--text);
        background: var(--panel-2);
        border: 1px solid var(--border);
        border-radius: 9px;
        cursor: pointer;
        transition: border-color 0.15s, transform 0.05s;
      }
      .login__btn:hover:not(:disabled) {
        border-color: var(--accent);
      }
      .login__btn:active:not(:disabled) {
        transform: translateY(1px);
      }
      .login__btn:disabled {
        opacity: 0.6;
        cursor: progress;
      }
      .login__btn-icon {
        display: inline-flex;
        width: 18px;
        height: 18px;
      }
      .login__error {
        margin: 0 0 1rem;
        padding: 0.6rem 0.75rem;
        font-size: 0.85rem;
        color: #f85149;
        background: rgba(248, 81, 73, 0.1);
        border: 1px solid rgba(248, 81, 73, 0.3);
        border-radius: 8px;
      }
      .login__hint {
        margin: 0.6rem 0 0;
        font-size: 0.78rem;
        text-align: center;
        color: var(--text-dim);
      }
      .login__legal {
        margin: 1.5rem 0 0;
        font-size: 0.72rem;
        line-height: 1.4;
        color: var(--text-dim);
        text-align: center;
      }
    `,
  ],
})
export class LoginComponent {
  private readonly auth = inject(AuthService);

  /** provider em andamento (desabilita os botões) ou null. */
  readonly loading = signal<Provider | null>(null);
  readonly error = signal<string | null>(null);

  /** Token Turnstile válido. null enquanto pendente/expirado. */
  private readonly captchaToken = signal<string | null>(null);

  /** Habilita os botões só quando o CAPTCHA resolveu. */
  readonly captchaReady = computed(() => this.captchaToken() !== null);

  onCaptchaToken(token: string | null): void {
    this.captchaToken.set(token);
  }

  readonly buttons: SocialButton[] = [
    {
      provider: 'google',
      label: 'Google',
      icon: `<svg viewBox="0 0 24 24" width="18" height="18"><path fill="#4285F4" d="M22.5 12.2c0-.7-.1-1.4-.2-2H12v3.8h5.9a5 5 0 0 1-2.2 3.3v2.7h3.6c2.1-1.9 3.2-4.8 3.2-7.8z"/><path fill="#34A853" d="M12 23c2.9 0 5.4-1 7.2-2.6l-3.6-2.7c-1 .7-2.3 1.1-3.6 1.1-2.8 0-5.1-1.9-6-4.4H2.3v2.8A10.9 10.9 0 0 0 12 23z"/><path fill="#FBBC05" d="M6 14.4a6.5 6.5 0 0 1 0-4.2V7.4H2.3a10.9 10.9 0 0 0 0 9.8L6 14.4z"/><path fill="#EA4335" d="M12 5.6c1.6 0 3 .5 4.1 1.6l3.1-3.1A10.9 10.9 0 0 0 2.3 7.4L6 10.2c.9-2.5 3.2-4.6 6-4.6z"/></svg>`,
    },
    {
      provider: 'apple',
      label: 'Apple',
      icon: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M16.4 12.6c0-2.2 1.8-3.3 1.9-3.4-1-1.5-2.6-1.7-3.2-1.7-1.4-.1-2.6.8-3.3.8-.7 0-1.7-.8-2.8-.8-1.5 0-2.8.8-3.6 2.2-1.5 2.7-.4 6.6 1.1 8.8.7 1 1.6 2.2 2.7 2.2 1.1 0 1.5-.7 2.8-.7 1.3 0 1.6.7 2.8.7 1.1 0 1.9-1 2.6-2 .8-1.2 1.2-2.3 1.2-2.4-.1 0-2.2-.9-2.2-3.4zM14.3 5.9c.6-.7 1-1.7.9-2.7-.9 0-1.9.6-2.5 1.3-.5.6-1 1.6-.9 2.6 1 .1 1.9-.5 2.5-1.2z"/></svg>`,
    },
    {
      provider: 'github',
      label: 'GitHub',
      icon: `<svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor"><path d="M12 2A10 10 0 0 0 2 12c0 4.4 2.9 8.2 6.8 9.5.5.1.7-.2.7-.5v-1.7c-2.8.6-3.4-1.3-3.4-1.3-.4-1.2-1.1-1.5-1.1-1.5-.9-.6.1-.6.1-.6 1 .1 1.5 1 1.5 1 .9 1.5 2.3 1.1 2.9.8.1-.6.3-1.1.6-1.3-2.2-.300-4.5-1.1-4.5-5 0-1.1.4-2 1-2.7-.1-.3-.4-1.3.1-2.7 0 0 .8-.3 2.7 1a9.4 9.4 0 0 1 5 0c1.9-1.3 2.7-1 2.7-1 .5 1.4.2 2.4.1 2.7.6.7 1 1.6 1 2.7 0 3.9-2.3 4.7-4.5 5 .4.3.7.9.7 1.8v2.6c0 .3.2.6.7.5A10 10 0 0 0 22 12 10 10 0 0 0 12 2z"/></svg>`,
    },
  ];

  async signIn(provider: Provider): Promise<void> {
    if (this.loading() !== null) {
      return;
    }
    const token = this.captchaToken();
    if (!token) {
      this.error.set('Resolva a verificação anti-bot antes de entrar.');
      return;
    }
    this.loading.set(provider);
    this.error.set(null);

    const { error } = await this.auth.loginWith(provider, token);
    if (error) {
      this.error.set(error.message ?? 'Falha ao iniciar o login. Tente novamente.');
      this.loading.set(null);
      // Token Turnstile é single-use; invalida pra forçar re-challenge.
      this.captchaToken.set(null);
    }
    // Em caso de sucesso o browser é redirecionado ao provider; nada a fazer.
  }
}
