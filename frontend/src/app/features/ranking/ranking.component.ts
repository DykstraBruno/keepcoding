import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { RankingService } from '../../services/ranking.service';
import {
  RankingEntry,
  RankingResponse,
  RankingSortKey,
} from '../../models/ranking.model';

/**
 * Ranking global do KeepCoding.
 *
 * Tabela única com username · XP · resolvidos Fácil/Médio/Difícil.
 * Backend já devolve ordenado por XP (com posições 1..N). Frontend permite
 * reordenar localmente clicando nos cabeçalhos de XP / Fácil / Médio / Difícil.
 * A coluna "Posição" volta sempre à ordem oficial (por XP).
 */
@Component({
  selector: 'app-ranking',
  imports: [],
  templateUrl: './ranking.component.html',
  styleUrl: './ranking.component.scss',
})
export class RankingComponent implements OnInit {
  private readonly rankingService = inject(RankingService);
  private readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly response = signal<RankingResponse | null>(null);

  /** Coluna ordenada localmente; null = ordem oficial do backend (por XP). */
  readonly sortKey = signal<RankingSortKey>('position');
  readonly sortDesc = signal<boolean>(false);

  /** ID do usuário logado pra destaque visual. */
  readonly myUserId = computed(() => this.auth.currentUser()?.userId ?? null);

  /** Top ordenado conforme {@link sortKey}/{@link sortDesc}. */
  readonly sortedTop = computed<RankingEntry[]>(() => {
    const top = this.response()?.top ?? [];
    const key = this.sortKey();
    const desc = this.sortDesc();
    const arr = [...top].sort((a, b) => {
      if (key === 'position') {
        return desc ? b.position - a.position : a.position - b.position;
      }
      const diff = (b[key] as number) - (a[key] as number);
      return desc ? diff : -diff;
    });
    return arr;
  });

  ngOnInit(): void {
    this.rankingService.get().subscribe({
      next: (res) => {
        this.response.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  /** Click no header. Mesma coluna toggla asc/desc; nova coluna começa desc. */
  toggleSort(key: RankingSortKey): void {
    if (this.sortKey() === key) {
      this.sortDesc.update((v) => !v);
    } else {
      this.sortKey.set(key);
      this.sortDesc.set(key !== 'position');
    }
  }

  /** Indicador textual da seta no header. */
  sortIndicator(key: RankingSortKey): string {
    if (this.sortKey() !== key) {
      return '';
    }
    return this.sortDesc() ? ' ↓' : ' ↑';
  }
}
