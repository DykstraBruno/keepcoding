import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Difficulty } from '../models/problem.model';
import {
  MatchState,
  MatchSubmitRequest,
} from '../models/match.model';

/** Acesso REST às partidas em tempo real. */
@Injectable({ providedIn: 'root' })
export class MatchService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/matches`;

  joinQueue(difficulty: Difficulty): Observable<void> {
    return this.http.post<void>(`${this.base}/queue`, { difficulty });
  }

  leaveQueue(): Observable<void> {
    return this.http.delete<void>(`${this.base}/queue`);
  }

  get(id: number): Observable<MatchState> {
    return this.http.get<MatchState>(`${this.base}/${id}`);
  }

  history(): Observable<MatchState[]> {
    return this.http.get<MatchState[]>(this.base);
  }

  submit(id: number, request: MatchSubmitRequest): Observable<MatchState> {
    return this.http.post<MatchState>(`${this.base}/${id}/submit`, request);
  }

  forfeit(id: number, reason: string): Observable<MatchState> {
    return this.http.post<MatchState>(`${this.base}/${id}/forfeit`, { reason });
  }
}
