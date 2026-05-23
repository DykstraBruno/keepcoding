import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProblemService } from '../../services/problem.service';
import { Difficulty, ProblemSummary } from '../../models/problem.model';

interface DifficultyGroup {
  difficulty: Difficulty;
  label: string;
  items: ProblemSummary[];
}

/** Dashboard pós-login: problemas de código agrupados por nível. */
@Component({
  selector: 'app-problem-list',
  imports: [RouterLink],
  templateUrl: './problem-list.component.html',
  styleUrl: './problem-list.component.scss',
})
export class ProblemListComponent implements OnInit {
  private readonly problemService = inject(ProblemService);

  readonly problems = signal<ProblemSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);

  /** Problemas agrupados em Fácil / Médio / Difícil. */
  readonly groups = computed<DifficultyGroup[]>(() => {
    const all = this.problems();
    const by = (d: Difficulty) => all.filter((p) => p.difficulty === d);
    return [
      { difficulty: 'EASY', label: 'Fácil', items: by('EASY') },
      { difficulty: 'MEDIUM', label: 'Médio', items: by('MEDIUM') },
      { difficulty: 'HARD', label: 'Difícil', items: by('HARD') },
    ];
  });

  ngOnInit(): void {
    this.problemService.getAll().subscribe({
      next: (list) => {
        this.problems.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
