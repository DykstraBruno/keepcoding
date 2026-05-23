import { Component, computed, input } from '@angular/core';
import { CoachClassification, CoachVerdict } from '../../../../models/coach.model';

/**
 * DevCoach: avatar + balão de fala + badge colorido com a nota da IA.
 * Genérico — recebe um {@link CoachVerdict}, servindo tanto para a análise
 * de código quanto para a de arquitetura.
 */
@Component({
  selector: 'app-dev-coach',
  imports: [],
  templateUrl: './dev-coach.component.html',
  styleUrl: './dev-coach.component.scss',
})
export class DevCoachComponent {
  /** Veredito a exibir. null = ainda sem análise. */
  readonly verdict = input<CoachVerdict | null>(null);

  /** true enquanto a submissão está sendo processada. */
  readonly loading = input<boolean>(false);

  /** Classe CSS do badge conforme a classificação. */
  readonly badgeClass = computed<string>(() => {
    const map: Record<CoachClassification, string> = {
      Brilhante: 'badge--brilhante',
      'Ótimo': 'badge--otimo',
      Livro: 'badge--livro',
      Incompleto: 'badge--incompleto',
      Gafe: 'badge--gafe',
    };
    const v = this.verdict();
    return v ? map[v.classification] ?? 'badge--livro' : 'badge--livro';
  });
}
