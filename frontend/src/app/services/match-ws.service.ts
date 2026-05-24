import { Injectable, inject } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { MatchEvent, MatchState, QueueStatus } from '../models/match.model';

/**
 * Cliente STOMP do KeepCoding.
 *
 * Conecta sob demanda em {@code /ws} com o JWT no header CONNECT.
 * Expõe três fluxos:
 *   - {@link queueStatus$}    — status da fila ({@code /user/queue/match}).
 *   - {@link matchFound$}     — partida formada ({@code /user/queue/match-found}).
 *   - {@link matchEvents$}    — eventos da partida ativa ({@code /topic/match/{id}}).
 *
 * O serviço cuida das subscriptions: cada chamada de {@link subscribeMatch}
 * troca a subscription anterior por uma nova, e {@link disconnect} libera tudo.
 */
@Injectable({ providedIn: 'root' })
export class MatchWsService {
  private readonly auth = inject(AuthService);

  private client: Client | null = null;
  private userQueueSub: StompSubscription | null = null;
  private userMatchFoundSub: StompSubscription | null = null;
  private matchTopicSub: StompSubscription | null = null;
  private currentMatchId: number | null = null;

  readonly queueStatus$ = new Subject<QueueStatus>();
  readonly matchFound$ = new Subject<MatchState>();
  readonly matchEvents$ = new Subject<MatchEvent>();
  readonly connected$ = new Subject<boolean>();

  /** Garante uma conexão ativa. Reutiliza a existente se já estiver conectada. */
  ensureConnected(): Promise<void> {
    if (this.client?.connected) {
      return Promise.resolve();
    }
    return new Promise((resolve, reject) => {
      const token = this.auth.token;
      if (!token) {
        reject(new Error('Sem token JWT — faça login antes de duelar.'));
        return;
      }

      // URL do endpoint /ws — http(s) -> ws(s).
      const apiBase = environment.apiUrl;
      const wsUrl = apiBase.replace(/^http/, 'ws') + '/ws';

      this.client = new Client({
        brokerURL: wsUrl,
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 0, // sem reconexão automática (UX previsível)
        onConnect: () => {
          this.connected$.next(true);
          this.subscribeUserChannels();
          resolve();
        },
        onStompError: (frame) => {
          this.connected$.next(false);
          reject(new Error('STOMP error: ' + frame.headers['message']));
        },
        onWebSocketClose: () => {
          this.connected$.next(false);
        },
      });
      this.client.activate();
    });
  }

  /** Pede uma partida (envia ao servidor via STOMP). */
  joinQueue(difficulty: 'EASY' | 'MEDIUM' | 'HARD'): void {
    this.client?.publish({
      destination: '/app/queue/join',
      body: JSON.stringify({ difficulty }),
    });
  }

  /** Cancela a busca por partida. */
  leaveQueue(): void {
    this.client?.publish({ destination: '/app/queue/leave' });
  }

  /** Assina o tópico da partida em andamento. Troca subscription anterior. */
  subscribeMatch(matchId: number): void {
    if (this.currentMatchId === matchId && this.matchTopicSub) {
      return;
    }
    this.matchTopicSub?.unsubscribe();
    this.matchTopicSub = this.client?.subscribe(`/topic/match/${matchId}`, (msg) =>
      this.matchEvents$.next(JSON.parse(msg.body) as MatchEvent),
    ) ?? null;
    this.currentMatchId = matchId;
  }

  unsubscribeMatch(): void {
    this.matchTopicSub?.unsubscribe();
    this.matchTopicSub = null;
    this.currentMatchId = null;
  }

  /** Encerra a conexão STOMP e descarta subscriptions. */
  disconnect(): void {
    this.userQueueSub?.unsubscribe();
    this.userMatchFoundSub?.unsubscribe();
    this.matchTopicSub?.unsubscribe();
    this.userQueueSub = null;
    this.userMatchFoundSub = null;
    this.matchTopicSub = null;
    this.currentMatchId = null;
    this.client?.deactivate();
    this.client = null;
    this.connected$.next(false);
  }

  // ---------------------------------------------------------------- internals
  private subscribeUserChannels(): void {
    if (!this.client) {
      return;
    }
    this.userQueueSub?.unsubscribe();
    this.userMatchFoundSub?.unsubscribe();

    this.userQueueSub = this.client.subscribe('/user/queue/match', (msg: IMessage) => {
      this.queueStatus$.next(JSON.parse(msg.body) as QueueStatus);
    });
    this.userMatchFoundSub = this.client.subscribe(
      '/user/queue/match-found',
      (msg: IMessage) => {
        this.matchFound$.next(JSON.parse(msg.body) as MatchState);
      },
    );
  }
}
