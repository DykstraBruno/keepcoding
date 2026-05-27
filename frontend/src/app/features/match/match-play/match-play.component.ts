import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { CodeEditorComponent } from '../../problem-detail/components/code-editor/code-editor.component';
import { AuthService } from '../../../services/auth.service';
import { MatchService } from '../../../services/match.service';
import { OpenAiKeyService } from '../../../services/openai-key.service';
import { OpenAiKeyDialogService } from '../../../services/openai-key-dialog.service';
import { MatchWsService } from '../../../services/match-ws.service';
import { MatchEvent, MatchState } from '../../../models/match.model';
import {
  DEFAULT_EDITOR_LANGUAGE,
  EditorLanguage,
  metaFor,
} from '../../../data/languages.catalog';

type Phase = 'PREFLIGHT' | 'PLAYING' | 'ENDED';

/**
 * Tela do duelo em tempo real com anti-cheat:
 *
 * 1. PREFLIGHT — usuário clica "Estou pronto":
 *    a. Verifica monitores via {@code window.getScreenDetails()};
 *       bloqueia se houver mais de 1 ou se a permissão for negada.
 *    b. Pede fullscreen no documento.
 *    c. Anexa listeners: visibilitychange, blur, fullscreenchange.
 * 2. PLAYING — editor + botão Submeter. Recebe eventos do oponente via STOMP.
 *    Qualquer violação → POST /forfeit + sai de fullscreen + tela "violação".
 * 3. ENDED — partida encerrada (vitória/derrota/forfeit). Mostra resultado.
 */
@Component({
  selector: 'app-match-play',
  imports: [CodeEditorComponent, RouterLink],
  templateUrl: './match-play.component.html',
  styleUrl: './match-play.component.scss',
})
export class MatchPlayComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(MatchService);
  private readonly ws = inject(MatchWsService);
  private readonly auth = inject(AuthService);
  private readonly openAiKey = inject(OpenAiKeyService);
  private readonly keyDialog = inject(OpenAiKeyDialogService);
  private promptedKey = false;

  private matchId = 0;
  private exitingProgrammatically = false;
  private listenersAttached = false;
  private eventsSub?: Subscription;

  // Bound handlers (mesma referência pra add/removeEventListener).
  private readonly onVis = () => this.handleVisibility();
  private readonly onBlur = () => this.violate('window_blur');
  private readonly onFsChange = () => this.handleFsChange();

  readonly phase = signal<Phase>('PREFLIGHT');
  readonly preflightError = signal<string | null>(null);
  readonly checking = signal(false);
  readonly loadError = signal(false);

  readonly match = signal<MatchState | null>(null);
  readonly language = signal<EditorLanguage>(DEFAULT_EDITOR_LANGUAGE);
  readonly code = signal<string>(metaFor(DEFAULT_EDITOR_LANGUAGE).starter);
  readonly running = signal(false);
  readonly lastEvent = signal<MatchEvent | null>(null);
  readonly violationReason = signal<string | null>(null);

  /** true se a partida está pronta pra render (já carregou estado). */
  readonly loaded = computed(() => this.match() !== null);

  /** Nome do oponente, do ponto de vista do usuário logado. */
  readonly opponentName = computed(() => {
    const m = this.match();
    const me = this.auth.currentUser();
    if (!m || !me) {
      return '';
    }
    return m.player1.id === me.userId ? m.player2.username : m.player1.username;
  });

  /** true se o usuário logado é o vencedor. */
  readonly amIWinner = computed(() => {
    const m = this.match();
    const me = this.auth.currentUser();
    return !!(m?.winner && me && m.winner.id === me.userId);
  });

  /** Mensagem do painel final (vitória / derrota / forfeit). */
  readonly endMessage = computed(() => {
    const m = this.match();
    if (!m || m.status === 'ACTIVE') {
      return '';
    }
    if (m.status === 'ABANDONED') {
      return this.amIWinner()
        ? `Vitória! Seu oponente forfeitou (${m.forfeitReason ?? 'motivo desconhecido'}).`
        : `Derrota por forfeit (${this.violationReason() ?? m.forfeitReason ?? 'violação'}).`;
    }
    return this.amIWinner() ? 'Vitória! Você acertou primeiro.' : `Derrota. ${m.winner?.username} acertou primeiro.`;
  });

  ngOnInit(): void {
    this.matchId = Number(this.route.snapshot.paramMap.get('id'));
    this.service.get(this.matchId).subscribe({
      next: (m) => {
        this.match.set(m);
        if (m.status !== 'ACTIVE') {
          this.phase.set('ENDED');
        }
      },
      error: () => this.loadError.set(true),
    });
  }

  /** Chamado pelo botão "Estou pronto" (necessário pra requestFullscreen). */
  async startPreflight(): Promise<void> {
    this.checking.set(true);
    this.preflightError.set(null);

    const monitor = await this.checkSingleScreen();
    if (!monitor.ok) {
      this.preflightError.set(monitor.reason);
      this.checking.set(false);
      return;
    }

    try {
      await document.documentElement.requestFullscreen();
    } catch (e: unknown) {
      this.preflightError.set(
        'Não foi possível entrar em tela cheia: ' +
          (e instanceof Error ? e.message : String(e)),
      );
      this.checking.set(false);
      return;
    }

    try {
      await this.ws.ensureConnected();
    } catch {
      // OK seguir sem WS — submissão funciona via REST mesmo sem realtime.
    }
    this.ws.subscribeMatch(this.matchId);
    this.eventsSub?.unsubscribe();
    this.eventsSub = this.ws.matchEvents$.subscribe((e) => this.handleMatchEvent(e));

    this.attachListeners();
    this.phase.set('PLAYING');
    this.checking.set(false);
  }

  async submit(): Promise<void> {
    if (this.running() || this.phase() !== 'PLAYING') {
      return;
    }
    if (!this.openAiKey.hasKey() && !this.promptedKey) {
      this.promptedKey = true;
      await this.keyDialog.requestKey();
    }
    this.running.set(true);
    this.service
      .submit(this.matchId, {
        language: metaFor(this.language()).api,
        code: this.code(),
      })
      .subscribe({
        next: (m) => {
          this.match.set(m);
          this.running.set(false);
          if (m.status !== 'ACTIVE') {
            this.endMatchClient();
          }
        },
        error: () => this.running.set(false),
      });
  }

  /** Desiste voluntariamente. */
  giveUp(): void {
    if (this.phase() !== 'PLAYING') {
      return;
    }
    this.violate('give_up');
  }

  ngOnDestroy(): void {
    this.removeListeners();
    this.eventsSub?.unsubscribe();
    this.ws.unsubscribeMatch();
    this.exitFullscreenSafe();
  }

  // ============================================================ Anti-cheat
  private async checkSingleScreen(): Promise<{ ok: boolean; reason: string }> {
    const w = window as unknown as {
      getScreenDetails?: () => Promise<{ screens: unknown[] }>;
    };
    if (typeof w.getScreenDetails !== 'function') {
      return {
        ok: false,
        reason:
          'Seu navegador não suporta detecção de monitores externos. Use Chrome ou Edge desktop atualizados.',
      };
    }
    try {
      const details = await w.getScreenDetails();
      const n = details?.screens?.length ?? 0;
      if (n === 0) {
        return { ok: false, reason: 'Não consegui ler informações de tela.' };
      }
      if (n > 1) {
        return {
          ok: false,
          reason: `Detectamos ${n} monitores conectados. Desconecte os monitores secundários antes de começar o duelo.`,
        };
      }
      return { ok: true, reason: '' };
    } catch {
      return {
        ok: false,
        reason:
          'Permissão de gerenciamento de janelas negada. Recarregue a página e aceite a permissão para entrar no duelo.',
      };
    }
  }

  private attachListeners(): void {
    if (this.listenersAttached) {
      return;
    }
    document.addEventListener('visibilitychange', this.onVis);
    window.addEventListener('blur', this.onBlur);
    document.addEventListener('fullscreenchange', this.onFsChange);
    this.listenersAttached = true;
  }

  private removeListeners(): void {
    if (!this.listenersAttached) {
      return;
    }
    document.removeEventListener('visibilitychange', this.onVis);
    window.removeEventListener('blur', this.onBlur);
    document.removeEventListener('fullscreenchange', this.onFsChange);
    this.listenersAttached = false;
  }

  private handleVisibility(): void {
    if (document.visibilityState === 'hidden') {
      this.violate('tab_switch');
    }
  }

  private handleFsChange(): void {
    if (this.exitingProgrammatically) {
      return;
    }
    if (!document.fullscreenElement) {
      this.violate('fullscreen_exit');
    }
  }

  /** Forfeit do lado cliente — registra motivo, POST forfeit, sai de fullscreen. */
  private violate(reason: string): void {
    if (this.phase() !== 'PLAYING' || this.violationReason()) {
      return;
    }
    this.violationReason.set(reason);
    this.removeListeners();
    this.exitFullscreenSafe();
    this.service.forfeit(this.matchId, reason).subscribe({
      next: (m) => {
        this.match.set(m);
        this.phase.set('ENDED');
      },
      error: () => this.phase.set('ENDED'),
    });
  }

  private async exitFullscreenSafe(): Promise<void> {
    if (!document.fullscreenElement) {
      return;
    }
    this.exitingProgrammatically = true;
    try {
      await document.exitFullscreen();
    } catch {
      /* sem-op */
    }
    this.exitingProgrammatically = false;
  }

  // ============================================================ Eventos WS
  private handleMatchEvent(event: MatchEvent): void {
    this.lastEvent.set(event);
    if (event.type === 'WIN' || event.type === 'FORFEIT' || event.type === 'MATCH_ENDED') {
      this.endMatchClient();
      this.service.get(this.matchId).subscribe((m) => this.match.set(m));
    }
  }

  private endMatchClient(): void {
    this.removeListeners();
    this.exitFullscreenSafe();
    this.phase.set('ENDED');
  }
}
