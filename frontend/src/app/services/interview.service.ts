import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  AnswerRequest,
  InterviewDetail,
  InterviewSummary,
  InterviewTurnResponse,
  StartInterviewRequest,
} from '../models/interview.model';

/** Acesso ao módulo de entrevistas com IA. */
@Injectable({ providedIn: 'root' })
export class InterviewService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/interviews`;

  list(): Observable<InterviewSummary[]> {
    return this.http.get<InterviewSummary[]>(this.base);
  }

  get(id: number): Observable<InterviewDetail> {
    return this.http.get<InterviewDetail>(`${this.base}/${id}`);
  }

  start(request: StartInterviewRequest): Observable<InterviewTurnResponse> {
    return this.http.post<InterviewTurnResponse>(`${this.base}/start`, request);
  }

  answer(id: number, request: AnswerRequest): Observable<InterviewTurnResponse> {
    return this.http.post<InterviewTurnResponse>(`${this.base}/${id}/answer`, request);
  }
}
