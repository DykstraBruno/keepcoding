import { Component, ViewChild, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ConsentService } from '../../services/consent.service';
import { CookiePreferencesComponent } from './cookie-preferences.component';

/**
 * Banner LGPD que aparece até o usuário decidir (aceitar, rejeitar ou
 * customizar). Não bloqueia a navegação — fica fixado no rodapé.
 */
@Component({
  selector: 'app-cookie-banner',
  imports: [RouterLink, CookiePreferencesComponent],
  template: `
    @if (visible()) {
      <aside class="banner" role="region" aria-label="Aviso de cookies">
        <div class="banner__text">
          <strong>Privacidade:</strong> usamos armazenamento local apenas para
          manter seu login (Supabase) e a chave de IA que você configurou
          (BYOK). Sem analytics, sem marketing, sem cookies de terceiros. Veja
          a <a routerLink="/privacidade">política completa</a>.
        </div>
        <div class="banner__actions">
          <button type="button" class="btn-link" (click)="openPrefs()">
            Personalizar
          </button>
          <button type="button" class="btn-secondary" (click)="reject()">
            Rejeitar não-essenciais
          </button>
          <button type="button" class="btn-primary" (click)="accept()">
            Aceitar todos
          </button>
        </div>
      </aside>
    }
    <app-cookie-preferences #prefs />
  `,
  styles: [
    `
      .banner {
        position: fixed;
        left: 1rem;
        right: 1rem;
        bottom: 1rem;
        z-index: 1050;
        display: flex;
        gap: 1rem;
        padding: 0.85rem 1rem;
        background: var(--panel);
        border: 1px solid var(--accent);
        border-radius: 10px;
        box-shadow: 0 16px 40px rgba(0, 0, 0, 0.55);
        align-items: center;
        flex-wrap: wrap;
      }
      .banner__text {
        flex: 1 1 320px;
        font-size: 0.85rem;
        color: var(--text);
        line-height: 1.5;
      }
      .banner__text a {
        color: var(--accent);
      }
      .banner__actions {
        display: flex;
        gap: 0.5rem;
        flex-wrap: wrap;
      }
      .btn-primary,
      .btn-secondary,
      .btn-link {
        padding: 0.5rem 0.95rem;
        font-size: 0.85rem;
        font-weight: 600;
        border-radius: 7px;
        cursor: pointer;
      }
      .btn-primary {
        color: #000;
        background: var(--accent);
        border: none;
      }
      .btn-primary:hover {
        filter: brightness(1.1);
      }
      .btn-secondary {
        color: var(--text);
        background: transparent;
        border: 1px solid var(--border);
      }
      .btn-secondary:hover {
        border-color: var(--accent);
      }
      .btn-link {
        background: transparent;
        border: none;
        color: var(--text-dim);
        padding: 0.5rem 0.5rem;
      }
      .btn-link:hover {
        color: var(--accent);
      }
    `,
  ],
})
export class CookieBannerComponent {
  private readonly consent = inject(ConsentService);

  @ViewChild('prefs') private prefs!: CookiePreferencesComponent;

  /** Visível enquanto o usuário não tomou nenhuma decisão. */
  readonly visible = computed(() => !this.consent.decided());

  accept(): void {
    this.consent.acceptAll();
  }

  reject(): void {
    this.consent.rejectNonEssential();
  }

  openPrefs(): void {
    this.prefs.show();
  }
}
