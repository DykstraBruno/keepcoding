import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CodeEditorComponent } from './components/code-editor/code-editor.component';
import { DevCoachComponent } from './components/dev-coach/dev-coach.component';
import { ProblemService } from '../../services/problem.service';
import { SubmissionService } from '../../services/submission.service';
import { AuthService } from '../../services/auth.service';
import { Problem } from '../../models/problem.model';
import { CoachVerdict } from '../../models/coach.model';
import { SubmissionResponse } from '../../models/submission.model';
import {
  DEFAULT_EDITOR_LANGUAGE,
  EditorLanguage,
  metaFor,
} from '../../data/languages.catalog';

/**
 * Tela do desafio. Layout dividido:
 *  - esquerda: descrição do problema + casos de teste de exemplo;
 *  - direita: editor de código, console de resultados e o DevCoach.
 */
@Component({
  selector: 'app-problem-detail',
  imports: [CodeEditorComponent, DevCoachComponent],
  templateUrl: './problem-detail.component.html',
  styleUrl: './problem-detail.component.scss',
})
export class ProblemDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly problemService = inject(ProblemService);
  private readonly submissionService = inject(SubmissionService);
  private readonly authService = inject(AuthService);

  // ---- Estado reativo (signals) ----
  readonly problem = signal<Problem | null>(null);
  readonly language = signal<EditorLanguage>(DEFAULT_EDITOR_LANGUAGE);
  readonly code = signal<string>(metaFor(DEFAULT_EDITOR_LANGUAGE).starter);
  readonly running = signal<boolean>(false);
  readonly result = signal<SubmissionResponse | null>(null);
  readonly loadError = signal<boolean>(false);

  // ---- Derivados ----
  readonly sampleCases = computed(
    () => this.problem()?.testCases.filter((tc) => tc.isSample) ?? [],
  );
  /** Feedback de código mapeado para o view-model genérico do DevCoach. */
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
        { label: 'Tempo', value: f.time_complexity },
        { label: 'Espaço', value: f.space_complexity },
      ],
      score: f.score,
    };
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.problemService.getById(id).subscribe({
      next: (p) => this.problem.set(p),
      error: () => this.loadError.set(true),
    });
  }

  /** Troca de linguagem: reinicia o código-modelo e limpa o resultado. */
  onLanguageChange(lang: EditorLanguage): void {
    this.language.set(lang);
    this.code.set(metaFor(lang).starter);
    this.result.set(null);
  }

  /** Envia a submissão ao backend. */
  onSubmit(): void {
    const problem = this.problem();
    if (!problem || this.running()) {
      return;
    }

    this.running.set(true);
    this.result.set(null);

    this.submissionService
      .submit({
        problemId: problem.id,
        language: metaFor(this.language()).api,
        code: this.code(),
      })
      .subscribe({
        next: (res) => {
          this.result.set(res);
          this.running.set(false);
          // Sincroniza o XP no header se a submissão concedeu pontos.
          this.authService.updateXp(res.userXp);
        },
        error: (err) => {
          console.error('Falha na submissão', err);
          this.running.set(false);
        },
      });
  }
}
