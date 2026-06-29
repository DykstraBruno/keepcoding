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

  /**
   * Dispara o fluxo OAuth do provider; o browser é redirecionado.
   *
   * @param captchaToken Token Turnstile validado no client. Para fluxo OAuth
   *   o Supabase Auth só consome o token em versões >= 2.151; nas versões
   *   anteriores o campo é ignorado e o CAPTCHA opera apenas como gate de UI
   *   (deterrente, não criptográfico). Para password/magic-link o token é
   *   sempre validado server-side via secret no painel.
   */
  signInWithOAuth(provider: Provider, captchaToken?: string) {
    const options: Record<string, unknown> = {
      redirectTo: `${window.location.origin}${environment.supabase.redirectPath}`,
    };
    if (captchaToken) {
      options['captchaToken'] = captchaToken;
    }
    return this.client.auth.signInWithOAuth({
      provider,
      options: options as Parameters<
        SupabaseClient['auth']['signInWithOAuth']
      >[0]['options'],
    });
  }

  signOut() {
    return this.client.auth.signOut();
  }
}
