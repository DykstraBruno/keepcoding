import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Provider } from '@supabase/supabase-js';
import { AuthUser } from '../models/auth.model';
import { SupabaseService } from './supabase.service';

/**
 * Fachada de autenticação sobre o Supabase Auth.
 *
 * <p>Mantém a superfície usada pelo app (currentUser, token, isAuthenticated,
 * updateXp, logout) para não quebrar componentes existentes, mas a fonte da
 * verdade é a sessão do {@link SupabaseService}. O backend (Spring) resolve o
 * usuário local pelo claim `email` do JWT do Supabase — por isso `userId` não
 * vem daqui.</p>
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly supabase = inject(SupabaseService);
  private readonly router = inject(Router);

  /** XP sobrescrito em memória após uma submissão ACCEPTED inédita. */
  private readonly xpOverride = signal<number | null>(null);

  /** Usuário logado (null = anônimo), derivado da sessão Supabase. */
  readonly currentUser = computed<AuthUser | null>(() => {
    const session = this.supabase.session();
    if (!session) {
      return null;
    }
    const u = session.user;
    const meta = (u.user_metadata ?? {}) as Record<string, string>;
    return {
      userId: 0, // resolvido por email no backend
      username:
        meta['full_name'] ??
        meta['name'] ??
        meta['user_name'] ??
        (u.email?.split('@')[0] ?? 'user'),
      email: u.email ?? '',
      tierPlan: 'FREE',
      xp: this.xpOverride() ?? 0,
      avatarUrl: meta['avatar_url'] ?? meta['picture'] ?? null,
    };
  });

  /** Inicia o login social; o browser é redirecionado para o provider. */
  loginWith(provider: Provider) {
    return this.supabase.signInWithOAuth(provider);
  }

  async logout(): Promise<void> {
    await this.supabase.signOut();
    this.xpOverride.set(null);
    this.router.navigate(['/login']);
  }

  /** Access token (JWT do Supabase), usado pelo interceptor HTTP. */
  get token(): string | null {
    return this.supabase.accessToken();
  }

  isAuthenticated(): boolean {
    return this.supabase.session() !== null;
  }

  /** Atualiza o XP exibido após uma submissão ACCEPTED inédita. */
  updateXp(newXp: number): void {
    this.xpOverride.set(newXp);
  }
}
