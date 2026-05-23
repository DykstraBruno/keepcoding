import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ArchitectureService } from '../../../services/architecture.service';
import {
  ArchitectureChallenge,
  ArchitectureSubmissionResponse,
} from '../../../models/architecture.model';
import { CoachVerdict } from '../../../models/coach.model';
import { DevCoachComponent } from '../../problem-detail/components/dev-coach/dev-coach.component';
import { MermaidPreviewComponent } from '../components/mermaid-preview/mermaid-preview.component';

/** Diagrama Mermaid inicial sugerido. */
const STARTER_MERMAID = `graph TD
  Cliente[Cliente] --> API[API Gateway]
  API --> Servico[Servico]
  Servico --> Banco[(Banco de Dados)]`;

/**
 * Tela do desafio de arquitetura. Layout dividido:
 *  - esquerda: contexto e requisitos;
 *  - direita: editor Mermaid + preview, justificativas e o DevCoach.
 */
@Component({
  selector: 'app-architecture-challenge',
  imports: [FormsModule, DevCoachComponent, MermaidPreviewComponent],
  templateUrl: './architecture-challenge.component.html',
  styleUrl: './architecture-challenge.component.scss',
})
export class ArchitectureChallengeComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly architectureService = inject(ArchitectureService);

  readonly challenge = signal<ArchitectureChallenge | null>(null);
  readonly mermaidCode = signal<string>(STARTER_MERMAID);
  readonly notes = signal<string>('');
  readonly running = signal<boolean>(false);
  readonly result = signal<ArchitectureSubmissionResponse | null>(null);
  readonly loadError = signal<boolean>(false);

  /** Feedback de arquitetura mapeado para o view-model do DevCoach. */
  readonly coachVerdict = computed<CoachVerdict | null>(() => {
    const f = this.result()?.coachFeedback;
    if (!f) {
      return null;
    }
    return {
      classification: f.classification,
      headline: f.headline,
      hint: f.socratic_hint,
      metrics: [
        { label: 'Escalabilidade', value: f.scalability },
        { label: 'Resiliência', value: f.resilience },
      ],
      score: f.score,
    };
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.architectureService.getChallenge(id).subscribe({
      next: (c) => this.challenge.set(c),
      error: () => this.loadError.set(true),
    });
  }

  onSubmit(): void {
    const challenge = this.challenge();
    if (!challenge || this.running()) {
      return;
    }

    this.running.set(true);
    this.result.set(null);

    this.architectureService
      .submit({
        challengeId: challenge.id,
        mermaidCode: this.mermaidCode(),
        notes: this.notes(),
      })
      .subscribe({
        next: (res) => {
          this.result.set(res);
          this.running.set(false);
        },
        error: (err) => {
          console.error('Falha na submissão de arquitetura', err);
          this.running.set(false);
        },
      });
  }
}
