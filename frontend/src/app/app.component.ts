import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';
import { ConnectionService } from './services/connection.service';
import { AiKeyService } from './services/ai-key.service';
import { ConnectionDialogService } from './features/connection/connection-dialog.service';
import { ConnectionDialogComponent } from './features/connection/connection-dialog.component';
import { CookieBannerComponent } from './features/consent/cookie-banner.component';
import { CookiePreferencesComponent } from './features/consent/cookie-preferences.component';

/** Shell da aplicação: cabeçalho com navegação + área roteada. */
@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    ConnectionDialogComponent,
    CookieBannerComponent,
    CookiePreferencesComponent,
  ],
  template: `
    <header class="app-header">
      <span class="app-logo">&lt;/&gt;</span>
      <span class="app-name">KeepCoding</span>

      @if (auth.currentUser(); as user) {
        <nav class="app-nav">
          <a
            routerLink="/"
            routerLinkActive="app-nav__link--active"
            [routerLinkActiveOptions]="{ exact: true }"
            class="app-nav__link"
            >Problemas</a
          >
          <a
            routerLink="/architecture"
            routerLinkActive="app-nav__link--active"
            class="app-nav__link"
            >Arquitetura</a
          >
          <a
            routerLink="/interviews"
            routerLinkActive="app-nav__link--active"
            class="app-nav__link"
            >Entrevistas</a
          >
          <a
            routerLink="/matches"
            routerLinkActive="app-nav__link--active"
            class="app-nav__link"
            >Duelos</a
          >
          <a
            routerLink="/ranking"
            routerLinkActive="app-nav__link--active"
            class="app-nav__link"
            >Ranking</a
          >
        </nav>
        <span class="app-spacer"></span>
        <button
          type="button"
          class="app-key"
          [class.app-key--set]="aiReady()"
          (click)="connectDialog.open()"
          [title]="aiReady() ? 'IA configurada — clique para gerenciar' : 'Conectar IA (OAuth ou chave própria)'">
          🤝 {{ aiReady() ? 'IA pronta' : 'Conectar IA' }}
        </button>
        <span class="app-user">{{ user.username }} · {{ user.xp }} XP</span>
        <button type="button" class="app-logout" (click)="auth.logout()">Sair</button>
      }
    </header>
    <main class="app-main">
      <router-outlet />
      <footer class="app-footer">
        <a routerLink="/privacidade" class="app-footer__link">Privacidade</a>
        <span class="app-footer__sep">·</span>
        <button type="button" class="app-footer__btn" (click)="prefs.show()">
          Preferências de cookies
        </button>
      </footer>
    </main>

    <app-connection-dialog />
    <app-cookie-banner />
    <app-cookie-preferences #prefs />
  `,
  styles: [
    `
      .app-header {
        display: flex;
        align-items: center;
        gap: 0.6rem;
        height: 52px;
        padding: 0 1.25rem;
        background: var(--panel);
        border-bottom: 1px solid var(--border);
      }
      .app-logo {
        color: var(--accent);
        font-weight: 800;
        font-family: monospace;
        font-size: 1.1rem;
      }
      .app-name {
        font-weight: 700;
        letter-spacing: 0.3px;
      }
      .app-nav {
        display: flex;
        gap: 0.25rem;
        margin-left: 1rem;
      }
      .app-nav__link {
        padding: 0.35rem 0.75rem;
        font-size: 0.85rem;
        color: var(--text-dim);
        text-decoration: none;
        border-radius: 6px;
      }
      .app-nav__link:hover {
        color: var(--text);
      }
      .app-nav__link--active {
        color: #fff;
        background: var(--panel-2);
      }
      .app-spacer {
        flex: 1;
      }
      .app-user {
        font-size: 0.85rem;
        color: var(--text-dim);
      }
      .app-logout {
        padding: 0.35rem 0.85rem;
        font-size: 0.82rem;
        color: var(--text);
        background: var(--panel-2);
        border: 1px solid var(--border);
        border-radius: 6px;
        cursor: pointer;
      }
      .app-logout:hover {
        border-color: var(--accent);
      }
      .app-key {
        padding: 0.35rem 0.75rem;
        font-size: 0.78rem;
        font-weight: 600;
        color: var(--text-dim);
        background: transparent;
        border: 1px solid var(--border);
        border-radius: 6px;
        cursor: pointer;
      }
      .app-key:hover {
        color: var(--text);
        border-color: var(--accent);
      }
      .app-key--set {
        color: var(--easy);
        border-color: rgba(63, 185, 80, 0.5);
      }
      .app-main {
        height: calc(100vh - 52px);
        overflow-y: auto;
        display: flex;
        flex-direction: column;
      }
      .app-main > router-outlet + * {
        flex: 1;
      }
      .app-footer {
        margin-top: auto;
        padding: 0.75rem 1.25rem;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;
        font-size: 0.78rem;
        color: var(--text-dim);
        border-top: 1px solid var(--border);
      }
      .app-footer__link {
        color: var(--text-dim);
        text-decoration: none;
      }
      .app-footer__link:hover {
        color: var(--accent);
      }
      .app-footer__sep {
        opacity: 0.5;
      }
      .app-footer__btn {
        background: transparent;
        border: none;
        color: var(--text-dim);
        font: inherit;
        cursor: pointer;
        padding: 0;
      }
      .app-footer__btn:hover {
        color: var(--accent);
      }
    `,
  ],
})
export class AppComponent implements OnInit {
  readonly auth = inject(AuthService);
  readonly connections = inject(ConnectionService);
  readonly aiKey = inject(AiKeyService);
  readonly connectDialog = inject(ConnectionDialogService);

  /** True se IA está pronta — OAuth conectado ou BYOK válido configurado. */
  readonly aiReady = (): boolean =>
    this.connections.connections().length > 0 || this.aiKey.isConfigured();

  async ngOnInit(): Promise<void> {
    // Carrega estado das conexões assim que o usuário entra.
    if (this.auth.currentUser()) {
      try {
        await this.connections.refresh();
      } catch {
        /* ignore — usuário pode não estar autenticado */
      }
    }
  }
}
