import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { InterviewService } from '../../../services/interview.service';
import { InterviewSummary } from '../../../models/interview.model';

/** Histórico de entrevistas do usuário. */
@Component({
  selector: 'app-interview-list',
  imports: [RouterLink, DatePipe],
  templateUrl: './interview-list.component.html',
  styleUrl: './interview-list.component.scss',
})
export class InterviewListComponent implements OnInit {
  private readonly interviewService = inject(InterviewService);

  readonly interviews = signal<InterviewSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);

  ngOnInit(): void {
    this.interviewService.list().subscribe({
      next: (list) => {
        this.interviews.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  statusLabel(status: InterviewSummary['status']): string {
    switch (status) {
      case 'IN_PROGRESS':
        return 'Em andamento';
      case 'COMPLETED':
        return 'Concluída';
      case 'ABANDONED':
        return 'Abandonada';
    }
  }
}
