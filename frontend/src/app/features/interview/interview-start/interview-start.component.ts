import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InterviewService } from '../../../services/interview.service';
import { OpenAiKeyService } from '../../../services/openai-key.service';
import { OpenAiKeyDialogService } from '../../../services/openai-key-dialog.service';

/** Tela inicial: candidato informa vaga + cola currículo e inicia a entrevista. */
@Component({
  selector: 'app-interview-start',
  imports: [FormsModule, RouterLink],
  templateUrl: './interview-start.component.html',
  styleUrl: './interview-start.component.scss',
})
export class InterviewStartComponent {
  private readonly interviewService = inject(InterviewService);
  private readonly router = inject(Router);
  private readonly openAiKey = inject(OpenAiKeyService);
  private readonly keyDialog = inject(OpenAiKeyDialogService);
  private promptedKey = false;

  readonly targetRole = signal('');
  readonly resumeText = signal('');
  /** Padrão 7 perguntas: 30 min de Q&A / 4 min cada ≈ 7. */
  readonly maxQuestions = signal(7);
  readonly starting = signal(false);
  readonly error = signal<string | null>(null);

  /** Bloqueia o submit até validação básica (vaga e currículo com tamanho mínimo). */
  readonly canSubmit = computed(
    () =>
      !this.starting() &&
      this.targetRole().trim().length > 0 &&
      this.resumeText().trim().length >= 50,
  );

  async start(): Promise<void> {
    if (!this.canSubmit()) {
      return;
    }
    if (!this.openAiKey.hasKey() && !this.promptedKey) {
      this.promptedKey = true;
      await this.keyDialog.requestKey();
    }
    this.starting.set(true);
    this.error.set(null);
    this.interviewService
      .start({
        targetRole: this.targetRole().trim(),
        resumeText: this.resumeText().trim(),
        maxQuestions: this.maxQuestions(),
      })
      .subscribe({
        next: (res) => this.router.navigate(['/interviews', res.interviewId]),
        error: (err) => {
          this.error.set(err?.error?.error ?? 'Não foi possível iniciar a entrevista.');
          this.starting.set(false);
        },
      });
  }
}
