/** Configuracao de ambiente (desenvolvimento). */
export const environment = {
  production: false,
  /** URL base da API do backend Spring Boot. */
  apiUrl: 'http://localhost:8080',

  /**
   * Supabase Auth. Preencha com os valores do seu projeto:
   * Dashboard → Project Settings → API.
   * `anonKey` é pública por design (segura no client); a service_role NUNCA
   * deve aparecer no frontend.
   */
  supabase: {
    url: 'https://YOUR_PROJECT_REF.supabase.co',
    anonKey: 'YOUR_SUPABASE_ANON_KEY',
    /** Rota que recebe o retorno do login social. */
    redirectPath: '/auth/callback',
  },

  /**
   * Cloudflare Turnstile (CAPTCHA do login).
   *
   * 1. Criar em dash.cloudflare.com/turnstile (free, sem limite).
   * 2. Copiar Site Key (pública, vai aqui) e Secret Key (vai no Supabase
   *    Dashboard → Authentication → Settings → Bot and Abuse Protection).
   * 3. Habilitar "Enable Captcha protection" no Supabase apontando o
   *    provider = Turnstile e colando a Secret Key.
   *
   * Em DEV pode usar a site key de teste do próprio Cloudflare:
   *   1x00000000000000000000AA  → sempre passa (visible)
   *   1x00000000000000000000BB  → sempre falha
   *   3x00000000000000000000FF  → desafio invisível (managed)
   */
  captcha: {
    turnstileSiteKey: '1x00000000000000000000AA',
  },
};
