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
};
