/**
 * LGPD — registro de consentimento granular do usuário.
 *
 * Necessários são sempre `true` (sessão de login, chave BYOK e o próprio
 * registro de consentimento) — sem eles o produto não funciona.
 * Os demais são opt-in explícito (default `false`).
 */

export type ConsentCategory =
  | 'necessary'
  | 'preferences'
  | 'analytics'
  | 'marketing';

export interface ConsentRecord {
  version: 1;
  givenAt: string;
  necessary: true;
  preferences: boolean;
  analytics: boolean;
  marketing: boolean;
}

export const CONSENT_CATEGORY_LABELS: Record<ConsentCategory, string> = {
  necessary: 'Necessários',
  preferences: 'Preferências',
  analytics: 'Análise de uso',
  marketing: 'Marketing',
};

export const CONSENT_CATEGORY_DESC: Record<ConsentCategory, string> = {
  necessary:
    'Indispensáveis ao funcionamento: sessão de login (Supabase) e sua chave de IA (BYOK), guardadas só no seu navegador.',
  preferences:
    'Lembram suas escolhas (tema, idioma). Não há cookies ativos nesta categoria hoje — reservada para evoluções futuras.',
  analytics:
    'Métricas anonimizadas para entendermos como o produto é usado. Não há ferramentas ativas hoje.',
  marketing:
    'Comunicação direcionada e personalização de campanhas. Não há ferramentas ativas hoje.',
};

export function defaultConsent(): ConsentRecord {
  return {
    version: 1,
    givenAt: new Date().toISOString(),
    necessary: true,
    preferences: false,
    analytics: false,
    marketing: false,
  };
}
