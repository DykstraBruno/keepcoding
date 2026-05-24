import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { RankingResponse } from '../models/ranking.model';

/** Acesso ao GET /api/ranking. */
@Injectable({ providedIn: 'root' })
export class RankingService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/ranking`;

  get(): Observable<RankingResponse> {
    return this.http.get<RankingResponse>(this.base);
  }
}
