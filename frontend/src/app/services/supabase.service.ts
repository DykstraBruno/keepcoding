import { Injectable, signal } from '@angular/core';
import {
  createClient,
  Provider,
  Session,
  SupabaseClient,
} from '@supabase/supabase-js';
import { environment } from '../../environments/environment';

/**
 * Cliente Supabase singleton + estado reativo da sessão.
 *
 * <p>É a única porta de entrada para o Supabase Auth. Mantém a sessão num
 * signal para leitura síncrona (interceptor) e expõe os métodos de login
 * social / logout. A persistência e o refresh do token são gerenciados pela
 * própria lib (`persistSession` + `autoRefreshToken`).</p>
 */
@Injectable({ providedIn: 'root' })
export class SupabaseService {
  private readonly client: SupabaseClient = createClient(
    environment.supabase.url,
    environment.supabase.anonKey,
    {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
        // Faz o Supabase consumir o token presente na URL do /auth/callback.
        detectSessionInUrl: true,
      },
    },
  );

  /** Sessão atual (null = anônimo). Atualizada em tempo real. */
  readonly session = signal<Session | null>(null);

  /** Resolve quando a sessão inicial (do storage/URL) terminou de carregar. */
  private readonly initialized: Promise<void>;

  constructor() {
    this.initialized = this.client.auth.getSession().then(({ data }) => {
      this.session.set(data.session);
    });
    this.client.auth.onAuthStateChange((_event, session) => {
      this.session.set(session);
    });
  }

  /** Aguarda a restauração inicial da sessão e devolve o estado. Usado pelo guard. */
  async ready(): Promise<Session | null> {
    await this.initialized;
    return this.session();
  }

  /** Access token (JWT do Supabase) para anexar no header Authorization. */
  accessToken(): string | null {
    return this.session()?.access_token ?? null;
  }

  /** Dispara o fluxo OAuth do provider; o browser é redirecionado. */
  signInWithOAuth(provider: Provider) {
    return this.client.auth.signInWithOAuth({
      provider,
      options: {
        redirectTo: `${window.location.origin}${environment.supabase.redirectPath}`,
      },
    });
  }

  signOut() {
    return this.client.auth.signOut();
  }
}
