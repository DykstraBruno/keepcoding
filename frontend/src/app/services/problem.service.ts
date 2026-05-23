import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Problem, ProblemSummary } from '../models/problem.model';

/** Acesso aos problemas de código via API. */
@Injectable({ providedIn: 'root' })
export class ProblemService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/problems`;

  /** Lista todos os problemas (resumo). */
  getAll(): Observable<ProblemSummary[]> {
    return this.http.get<ProblemSummary[]>(this.base);
  }

  /** Detalhe completo de um problema. */
  getById(id: number): Observable<Problem> {
    return this.http.get<Problem>(`${this.base}/${id}`);
  }
}
