/**
 * BYOK — chave de IA configurada pelo próprio usuário.
 *
 * A chave NUNCA sai do browser para o backend salvar — é enviada por header
 * (`X-AI-Key` + `X-AI-Provider`) em cada requisição que precisa de IA.
 */

export type AiKeyProvider = 'OPENAI' | 'GOOGLE';

export interface AiKeyConfig {
  provider: AiKeyProvider;
  apiKey: string;
}

/** Regex de validação alinhada com o backend ByokKeyFilter. */
const FORMAT: Record<AiKeyProvider, RegExp> = {
  OPENAI: /^sk-[A-Za-z0-9_-]{20,200}$/,
  GOOGLE: /^AIza[A-Za-z0-9_-]{30,80}$/,
};

export function isValidAiKey(cfg: AiKeyConfig | null): cfg is AiKeyConfig {
  if (!cfg) return false;
  const re = FORMAT[cfg.provider];
  return !!re && re.test(cfg.apiKey);
}

/** Para exibir em UI sem vazar a chave — só mostra prefixo e últimos 4. */
export function maskApiKey(key: string): string {
  if (!key || key.length < 10) return '••••';
  return `${key.slice(0, 4)}…${key.slice(-4)}`;
}

export const AI_PROVIDER_LABELS: Record<AiKeyProvider, string> = {
  OPENAI: 'OpenAI (ChatGPT)',
  GOOGLE: 'Google (Gemini)',
};

export const AI_PROVIDER_HINTS: Record<AiKeyProvider, string> = {
  OPENAI: 'Comece com sk-… Crie em platform.openai.com/api-keys',
  GOOGLE: 'Comece com AIza… Crie em aistudio.google.com/apikey',
};
