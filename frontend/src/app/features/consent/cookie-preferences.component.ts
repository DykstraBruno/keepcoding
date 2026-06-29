import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ConsentService } from '../../services/consent.service';
import {
  CONSENT_CATEGORY_DESC,
  CONSENT_CATEGORY_LABELS,
} from '../../models/consent.model';

/**
 * Modal de preferências de cookies / dados (LGPD).
 *
 * Permite ao usuário alternar cada categoria (exceto `necessary`, que é
 * sempre on) e gravar o consentimento granular. Reabre via footer ou pelo
 * banner inicial.
 */
@Component({
  selector: 'app-cookie-preferences',
  imports: [FormsModule, RouterLink],
  template: `
    @if (open()) {
      <div class="overlay" (click)="close()">
        <div class="modal" (click)="$event.stopPropagation()" role="dialog" aria-modal="true">
          <header class="modal__head">
            <h2 class="modal__title">Preferências de privacidade</h2>
            <p class="modal__sub">
              Controle quais categorias de dados podem ser tratadas. Os
              <b>necessários</b> mantêm o login e suas configurações funcionando
              e não podem ser desativados. Detalhes em
              <a routerLink="/privacidade" (click)="close()">/privacidade</a>.
            </p>
          </header>

          <ul class="cats">
            @for (c of categories; track c) {
              <li class="cat">
                <div class="cat__row">
                  <span class="cat__label">{{ labels[c] }}</span>
                  @if (c === 'necessary') {
                    <span class="badge badge--lock">sempre ativo</span>
                  } @else {
                    <label class="switch">
                      <input
                        type="checkbox"
                        [ngModel]="getToggle(c)"
                        (ngModelChange)="setToggle(c, $event)" />
                      <span class="switch__track"></span>
                    </label>
                  }
                </div>
                <p class="cat__desc">{{ descs[c] }}</p>
              </li>
            }
          </ul>

          <footer class="modal__actions">
            <button type="button" class="btn-secondary" (click)="rejectAll()">
              Rejeitar não-essenciais
            </button>
            <button type="button" class="btn-secondary" (click)="acceptAll()">
              Aceitar todos
            </button>
            <button type="button" class="btn-primary" (click)="save()">
              Salvar escolhas
            </button>
          </footer>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .overlay {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.6);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1100;
        padding: 1rem;
      }
      .modal {
        width: 100%;
        max-width: 560px;
        background: var(--panel);
        border: 1px solid var(--border);
        border-radius: 12px;
        padding: 1.5rem;
        display: flex;
        flex-direction: column;
        gap: 1rem;
        box-shadow: 0 24px 48px rgba(0, 0, 0, 0.55);
      }
      .modal__title {
        margin: 0;
        font-size: 1.2rem;
      }
      .modal__sub {
        margin: 0.25rem 0 0;
        color: var(--text-dim);
        font-size: 0.85rem;
        line-height: 1.45;
      }
      .modal__sub a {
        color: var(--accent);
      }
      .cats {
        list-style: none;
        padding: 0;
        margin: 0;
        display: flex;
        flex-direction: column;
        gap: 0.55rem;
      }
      .cat {
        padding: 0.7rem 0.9rem;
        background: var(--panel-2);
        border: 1px solid var(--border);
        border-radius: 9px;
      }
      .cat__row {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
      .cat__label {
        font-weight: 700;
      }
      .cat__desc {
        margin: 0.3rem 0 0;
        font-size: 0.78rem;
        color: var(--text-dim);
        line-height: 1.45;
      }
      .badge--lock {
        font-size: 0.65rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        padding: 0.2rem 0.55rem;
        border-radius: 999px;
        background: var(--panel);
        border: 1px solid var(--border);
        color: var(--text-dim);
      }
      .switch {
        position: relative;
        display: inline-block;
        width: 38px;
        height: 22px;
        cursor: pointer;
      }
      .switch input {
        opacity: 0;
        width: 0;
        height: 0;
      }
      .switch__track {
        position: absolute;
        inset: 0;
        background: var(--panel);
        border: 1px solid var(--border);
        border-radius: 999px;
        transition: 0.2s;
      }
      .switch__track::before {
        content: '';
        position: absolute;
        height: 16px;
        width: 16px;
        left: 2px;
        top: 2px;
        background: var(--text-dim);
        border-radius: 50%;
        transition: 0.2s;
      }
      .switch input:checked + .switch__track {
        background: rgba(0, 217, 122, 0.25);
        border-color: var(--accent);
      }
      .switch input:checked + .switch__track::before {
        transform: translateX(16px);
        background: var(--accent);
      }
      .modal__actions {
        display: flex;
        gap: 0.5rem;
        justify-content: flex-end;
        flex-wrap: wrap;
      }
      .btn-primary,
      .btn-secondary {
        padding: 0.5rem 1rem;
        font-size: 0.85rem;
        border-radius: 7px;
        cursor: pointer;
        font-weight: 600;
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
    `,
  ],
})
export class CookiePreferencesComponent {
  readonly consent = inject(ConsentService);

  readonly categories = ['necessary', 'preferences', 'analytics', 'marketing'] as const;
  readonly labels = CONSENT_CATEGORY_LABELS;
  readonly descs = CONSENT_CATEGORY_DESC;

  readonly open = signal(false);

  /** Estado local das toggles enquanto o modal está aberto (pré-save). */
  private readonly draft = signal({ preferences: false, analytics: false, marketing: false });

  /** Abre o modal sincronizando os toggles com o consentimento atual. */
  show(): void {
    const r = this.consent.record();
    this.draft.set({
      preferences: r?.preferences ?? false,
      analytics: r?.analytics ?? false,
      marketing: r?.marketing ?? false,
    });
    this.open.set(true);
  }

  close(): void {
    this.open.set(false);
  }

  getToggle(c: 'preferences' | 'analytics' | 'marketing'): boolean {
    return this.draft()[c];
  }

  setToggle(c: 'preferences' | 'analytics' | 'marketing', value: boolean): void {
    this.draft.update((d) => ({ ...d, [c]: value }));
  }

  save(): void {
    this.consent.saveCustom(this.draft());
    this.close();
  }

  acceptAll(): void {
    this.consent.acceptAll();
    this.close();
  }

  rejectAll(): void {
    this.consent.rejectNonEssential();
    this.close();
  }
}
