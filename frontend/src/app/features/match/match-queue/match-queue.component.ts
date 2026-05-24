import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Difficulty } from '../../../models/problem.model';
import { MatchState } from '../../../models/match.model';
import { MatchService } from '../../../services/match.service';
import { MatchWsService } from '../../../services/match-ws.service';

/** Tela de matchmaking: escolhe dificuldade, busca oponente, mostra histórico. */
@Component({
  selector: 'app-match-queue',
  imports: [RouterLink, DatePipe],
  templateUrl: './match-queue.component.html',
  styleUrl: './match-queue.component.scss',
})
export class MatchQueueComponent implements OnInit, OnDestroy {
  private readonly ws = inject(MatchWsService);
  private readonly service = inject(MatchService);
  private readonly router = inject(Router);

  readonly searching = signal(false);
  readonly difficulty = signal<Difficulty | null>(null);
  readonly statusMsg = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly history = signal<MatchState[]>([]);

  private matchFoundSub?: Subscription;
  private queueSub?: Subscription;

  ngOnInit(): void {
    this.service.history().subscribe({
      next: (h) => this.history.set(h),
      error: () => {},
    });
  }

  async startSearch(d: Difficulty): Promise<void> {
    this.error.set(null);
    this.statusMsg.set('Conectando…');
    try {
      await this.ws.ensureConnected();
    } catch (e: unknown) {
      this.error.set(e instanceof Error ? e.message : 'Falha ao conectar.');
      this.statusMsg.set(null);
      return;
    }
    this.difficulty.set(d);
    this.searching.set(true);
    this.statusMsg.set('Procurando oponente…');

    this.matchFoundSub?.unsubscribe();
    this.matchFoundSub = this.ws.matchFound$.subscribe((state) => {
      this.searching.set(false);
      this.router.navigate(['/matches', state.matchId]);
    });
    this.queueSub?.unsubscribe();
    this.queueSub = this.ws.queueStatus$.subscribe((s) => this.statusMsg.set(s.message));

    this.ws.joinQueue(d);
  }

  cancel(): void {
    if (!this.searching()) {
      return;
    }
    this.ws.leaveQueue();
    this.searching.set(false);
    this.statusMsg.set(null);
    this.difficulty.set(null);
  }

  ngOnDestroy(): void {
    this.matchFoundSub?.unsubscribe();
    this.queueSub?.unsubscribe();
    if (this.searching()) {
      this.ws.leaveQueue();
    }
  }
}
