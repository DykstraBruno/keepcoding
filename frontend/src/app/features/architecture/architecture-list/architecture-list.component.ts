import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ArchitectureService } from '../../../services/architecture.service';
import { ArchitectureChallengeSummary } from '../../../models/architecture.model';
import { Difficulty } from '../../../models/problem.model';

interface DifficultyGroup {
  difficulty: Difficulty;
  label: string;
  items: ArchitectureChallengeSummary[];
}

/** Lista de desafios de arquitetura agrupados por nível. */
@Component({
  selector: 'app-architecture-list',
  imports: [RouterLink],
  templateUrl: './architecture-list.component.html',
  styleUrl: './architecture-list.component.scss',
})
export class ArchitectureListComponent implements OnInit {
  private readonly architectureService = inject(ArchitectureService);

  readonly challenges = signal<ArchitectureChallengeSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);

  readonly groups = computed<DifficultyGroup[]>(() => {
    const all = this.challenges();
    const by = (d: Difficulty) => all.filter((c) => c.difficulty === d);
    return [
      { difficulty: 'EASY', label: 'Fácil', items: by('EASY') },
      { difficulty: 'MEDIUM', label: 'Médio', items: by('MEDIUM') },
      { difficulty: 'HARD', label: 'Difícil', items: by('HARD') },
    ];
  });

  ngOnInit(): void {
    this.architectureService.getChallenges().subscribe({
      next: (list) => {
        this.challenges.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }
}
