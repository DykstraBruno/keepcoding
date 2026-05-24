import {
  AfterViewChecked,
  Component,
  ElementRef,
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
import {
  InterviewFeedback,
  InterviewMessageView,
  InterviewStatus,
} from '../../../models/interview.model';

/** Tela de execução da entrevista — chat de turnos + feedback final. */
@Component({
  selector: 'app-interview-chat',
  imports: [FormsModule, RouterLink, DatePipe],
  templateUrl: './interview-chat.component.html',
  styleUrl: './interview-chat.component.scss',
})
export class InterviewChatComponent implements OnInit, AfterViewChecked {
  private readonly route = inject(ActivatedRoute);
  private readonly interviewService = inject(InterviewService);

  private interviewId = 0;

  readonly targetRole = signal<string | null>(null);
  readonly status = signal<InterviewStatus>('IN_PROGRESS');
  readonly messages = signal<InterviewMessageView[]>([]);
  readonly feedback = signal<InterviewFeedback | null>(null);
  readonly maxQuestions = signal<number>(6);
  readonly questionNumber = signal<number>(0);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly draft = signal('');
  readonly sending = signal(false);

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
        this.messages.set(detail.messages);
        this.feedback.set(detail.feedback);
        this.maxQuestions.set(detail.maxQuestions);
        this.questionNumber.set(
          detail.messages.filter((m) => m.role === 'INTERVIEWER').length,
        );
        this.loading.set(false);
        this.shouldScroll = true;
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
      },
    });
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

  send(): void {
    const text = this.draft().trim();
    if (!text || !this.canSend()) {
      return;
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
        if (turn.finished) {
          this.feedback.set(turn.feedback);
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
          this.questionNumber.set(turn.questionNumber);
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
}
