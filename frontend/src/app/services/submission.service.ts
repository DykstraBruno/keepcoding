import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SubmissionRequest, SubmissionResponse } from '../models/submission.model';

/** Comunicacao com o endpoint de submissoes do backend. */
@Injectable({ providedIn: 'root' })
export class SubmissionService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiUrl}/api/submissions`;

  /** Envia o codigo do usuario para execucao + analise do DevCoach. */
  submit(request: SubmissionRequest): Observable<SubmissionResponse> {
    return this.http.post<SubmissionResponse>(this.endpoint, request);
  }
}
