import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Connection,
  OAuthProvider,
  StartOAuthResponse,
} from '../models/connection.model';

/**
 * Estado das conexões OAuth do usuário + chamadas REST do fluxo.
 *
 * O signal {@link connections} é a fonte da verdade pra UI (header chip,
 * modal de gerenciamento). É recarregado após connect / disconnect.
 */
@Injectable({ providedIn: 'root' })
export class ConnectionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/oauth`;

  readonly connections = signal<Connection[]>([]);
  readonly loading = signal(false);

  /** Recarrega o estado das conexões do servidor. */
  async refresh(): Promise<void> {
    this.loading.set(true);
    try {
      const list = await firstValueFrom(
        this.http.get<Connection[]>(`${this.base}/connections`),
      );
      this.connections.set(list ?? []);
    } finally {
      this.loading.set(false);
    }
  }

  /** true se o provider está conectado nesta conta. */
  isConnected(provider: OAuthProvider): boolean {
    return this.connections().some((c) => c.provider === provider);
  }

  /** Devolve a conexão completa do provider, ou null. */
  get(provider: OAuthProvider): Connection | null {
    return this.connections().find((c) => c.provider === provider) ?? null;
  }

  /** Pede ao backend a URL de authorize e o state. */
  start(provider: OAuthProvider): Promise<StartOAuthResponse> {
    return firstValueFrom(
      this.http.post<StartOAuthResponse>(
        `${this.base}/${provider.toLowerCase()}/start`,
        {},
      ),
    );
  }

  /** Troca code+state por tokens persistidos no backend. */
  async exchange(provider: OAuthProvider, code: string, state: string): Promise<void> {
    await firstValueFrom(
      this.http.post<void>(`${this.base}/${provider.toLowerCase()}/exchange`, {
        code,
        state,
      }),
    );
    await this.refresh();
  }

  /** Desconecta um provider (remove o token do banco). */
  async disconnect(provider: OAuthProvider): Promise<void> {
    await firstValueFrom(
      this.http.delete<void>(`${this.base}/connections/${provider}`),
    );
    await this.refresh();
  }
}
