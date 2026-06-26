import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SupabaseService } from '../services/supabase.service';

/**
 * Bloqueia rotas protegidas; redireciona para /login se não houver sessão
 * Supabase. Assíncrono: aguarda a restauração inicial da sessão (storage/URL)
 * antes de decidir, evitando "flash" de logout num F5.
 */
export const authGuard: CanActivateFn = async () => {
  const supabase = inject(SupabaseService);
  const router = inject(Router);

  const session = await supabase.ready();
  return session ? true : router.createUrlTree(['/login']);
};
