import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './services/auth.service';

/** Shell da aplicação: cabeçalho com navegação + área roteada. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
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
        </nav>
        <span class="app-spacer"></span>
        <span class="app-user">{{ user.username }} · {{ user.xp }} XP</span>
        <button type="button" class="app-logout" (click)="auth.logout()">Sair</button>
      }
    </header>
    <main class="app-main">
      <router-outlet />
    </main>
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
      .app-main {
        height: calc(100vh - 52px);
        overflow-y: auto;
      }
    `,
  ],
})
export class AppComponent {
  readonly auth = inject(AuthService);
}
