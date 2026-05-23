import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ArchitectureChallenge,
  ArchitectureChallengeSummary,
  ArchitectureSubmissionRequest,
  ArchitectureSubmissionResponse,
} from '../models/architecture.model';

/** Acesso aos desafios de arquitetura via API. */
@Injectable({ providedIn: 'root' })
export class ArchitectureService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/architecture`;

  getChallenges(): Observable<ArchitectureChallengeSummary[]> {
    return this.http.get<ArchitectureChallengeSummary[]>(`${this.base}/challenges`);
  }

  getChallenge(id: number): Observable<ArchitectureChallenge> {
    return this.http.get<ArchitectureChallenge>(`${this.base}/challenges/${id}`);
  }

  submit(request: ArchitectureSubmissionRequest): Observable<ArchitectureSubmissionResponse> {
    return this.http.post<ArchitectureSubmissionResponse>(`${this.base}/submissions`, request);
  }
}
