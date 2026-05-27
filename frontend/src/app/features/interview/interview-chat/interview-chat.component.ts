import {
  AfterViewChecked,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { InterviewService } from '../../../services/interview.service';
import { OpenAiKeyService } from '../../../services/openai-key.service';
import { OpenAiKeyDialogService } from '../../../services/openai-key-dialog.service';
import {
  InterviewFeedback,
  InterviewMessageView,
  InterviewPhase,
  InterviewStatus,
} from '../../../models/interview.model';

/** Duração total da entrevista (40 min — padrão do mercado). */
const TOTAL_SECONDS = 40 * 60;
const PRESENTATION_SECONDS = 10 * 60;

/** Tela de execução da entrevista — chat de turnos + timer + feedback final. */
@Component({
  selector: 'app-interview-chat',
  imports: [FormsModule, RouterLink, DatePipe],
  templateUrl: './interview-chat.component.html',
  styleUrl: './interview-chat.component.scss',
})
export class InterviewChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  private readonly route = inject(ActivatedRoute);
  private readonly interviewService = inject(InterviewService);
  private readonly openAiKey = inject(OpenAiKeyService);
  private readonly keyDialog = inject(OpenAiKeyDialogService);
  private promptedKey = false;

  private interviewId = 0;
  private startEpochMs = 0;
  private tickHandle: ReturnType<typeof setInterval> | null = null;

  readonly targetRole = signal<string | null>(null);
  readonly status = signal<InterviewStatus>('IN_PROGRESS');
  readonly phase = signal<InterviewPhase>('PRESENTATION');
  readonly messages = signal<InterviewMessageView[]>([]);
  readonly feedback = signal<InterviewFeedback | null>(null);
  readonly maxQuestions = signal<number>(7);
  readonly questionNumber = signal<number>(1);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly draft = signal('');
  readonly sending = signal(false);

  /** Segundos decorridos desde createdAt. Atualizado por intervalo (1s). */
  readonly elapsedSeconds = signal(0);

  /** Restantes do orçamento total de 40 min (não trava UI; é guia visual). */
  readonly remainingSeconds = computed(() =>
    Math.max(0, TOTAL_SECONDS - this.elapsedSeconds()),
  );

  readonly elapsedLabel = computed(() => formatMMSS(this.elapsedSeconds()));
  readonly remainingLabel = computed(() => formatMMSS(this.remainingSeconds()));

  /** true se já passou do orçamento total — destaca o timer. */
  readonly overtime = computed(() => this.elapsedSeconds() > TOTAL_SECONDS);

  /** Rótulo da fase atual incluindo numeração quando em QUESTIONS. */
  readonly phaseLabel = computed(() => {
    switch (this.phase()) {
      case 'PRESENTATION':
        return '🎤 Apresentação (até 10 min)';
      case 'QUESTIONS':
        return `Pergunta ${this.questionNumber()} / ${this.maxQuestions()} (~4 min cada)`;
      case 'COMPLETED':
        return '✅ Encerrada';
    }
  });

  /** true se a entrevista ainda aceita respostas. */
  readonly active = computed(() => this.status() === 'IN_PROGRESS');

  /** true se há rascunho válido pra enviar. */
  readonly canSend = computed(
    () => this.active() && !this.sending() && this.draft().trim().length > 0,
  );

  private readonly scrollHost = viewChild<ElementRef<HTMLDivElement>>('scrollHost');
  private shouldScroll = false;

  ngOnInit(): void {
    this.interviewId = Number(this.route.snapshot.paramMap.get('id'));
    this.interviewService.get(this.interviewId).subscribe({
      next: (detail) => {
        this.targetRole.set(detail.targetRole);
        this.status.set(detail.status);
        this.phase.set(detail.phase);
        this.messages.set(detail.messages);
        this.feedback.set(detail.feedback);
        this.maxQuestions.set(detail.maxQuestions);

        const answers = detail.messages.filter((m) => m.role === 'CANDIDATE').length;
        // Em QUESTIONS, questionNumber = respostas dadas (presentation conta 1).
        this.questionNumber.set(Math.max(1, answers));

        this.startEpochMs = new Date(detail.createdAt).getTime();
        this.tickElapsed();
        if (detail.status === 'IN_PROGRESS') {
          this.tickHandle = setInterval(() => this.tickElapsed(), 1000);
        }

        this.loading.set(false);
        this.shouldScroll = true;
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    if (this.tickHandle) {
      clearInterval(this.tickHandle);
    }
  }

  ngAfterViewChecked(): void {
    if (!this.shouldScroll) {
      return;
    }
    const host = this.scrollHost()?.nativeElement;
    if (host) {
      host.scrollTop = host.scrollHeight;
    }
    this.shouldScroll = false;
  }

  async send(): Promise<void> {
    const text = this.draft().trim();
    if (!text || !this.canSend()) {
      return;
    }
    if (!this.openAiKey.hasKey() && !this.promptedKey) {
      this.promptedKey = true;
      await this.keyDialog.requestKey();
    }
    this.sending.set(true);

    // Otimista: já mostra a resposta do candidato.
    this.messages.update((list) => [
      ...list,
      {
        role: 'CANDIDATE',
        turnIndex: list.length,
        content: text,
        createdAt: new Date().toISOString(),
      },
    ]);
    this.draft.set('');
    this.shouldScroll = true;

    this.interviewService.answer(this.interviewId, { content: text }).subscribe({
      next: (turn) => {
        this.status.set(turn.status);
        this.phase.set(turn.phase);
        this.questionNumber.set(turn.questionNumber);

        if (turn.finished) {
          this.feedback.set(turn.feedback);
          if (this.tickHandle) {
            clearInterval(this.tickHandle);
            this.tickHandle = null;
          }
        } else if (turn.nextQuestion) {
          this.messages.update((list) => [
            ...list,
            {
              role: 'INTERVIEWER',
              turnIndex: list.length,
              content: turn.nextQuestion!,
              createdAt: new Date().toISOString(),
            },
          ]);
        }
        this.sending.set(false);
        this.shouldScroll = true;
      },
      error: (err) => {
        console.error('Falha ao enviar resposta', err);
        this.sending.set(false);
      },
    });
  }

  private tickElapsed(): void {
    if (!this.startEpochMs) {
      return;
    }
    const elapsed = Math.floor((Date.now() - this.startEpochMs) / 1000);
    this.elapsedSeconds.set(Math.max(0, elapsed));
  }
}

function formatMMSS(totalSeconds: number): string {
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return `${pad2(m)}:${pad2(s)}`;
}

function pad2(n: number): string {
  return n.toString().padStart(2, '0');
}

// Re-exporta a constante para o template (HTML não acessa const top-level).
export const INTERVIEW_PRESENTATION_SECONDS = PRESENTATION_SECONDS;
