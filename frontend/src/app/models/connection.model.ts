/** Providers de IA que aceitam OAuth público. Espelha o enum do backend. */
export type OAuthProvider = 'GOOGLE' | 'ANTHROPIC' | 'OPENAI';

/** Resposta de GET /api/oauth/connections. */
export interface Connection {
  provider: OAuthProvider;
  providerAccountEmail: string | null;
  connectedAt: string;
  expiresAt: string | null;
  scope: string | null;
}

/** Resposta de POST /api/oauth/{provider}/start. */
export interface StartOAuthResponse {
  authorizeUrl: string;
  state: string;
}

/** Resultado entregue via postMessage do popup pro opener. */
export interface OAuthPopupResult {
  status: 'success' | 'error' | 'closed';
  provider?: OAuthProvider;
  error?: string;
}
