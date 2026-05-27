import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { OpenAiKeyService } from './openai-key.service';

const AUTH_BASE = 'https://openrouter.ai/auth';
const TOKEN_EXCHANGE = 'https://openrouter.ai/api/v1/auth/keys';
const VERIFIER_STORAGE_KEY = 'kc_openrouter_verifier';

/**
 * Fluxo OAuth PKCE com o OpenRouter.
 *
 * O OpenRouter é um proxy compatível com a API OpenAI que tem um
 * fluxo OAuth público (PKCE) — algo que OpenAI/Anthropic/Google
 * não oferecem para acesso à API. Após o login, o app recebe uma
 * chave do tipo {@code sk-or-...} que é usada como o
 * {@code X-OpenAI-Key} comum.
 *
 * Fluxo:
 *  1. {@link startLogin}: gera code_verifier + code_challenge, salva o
 *     verifier no localStorage e redireciona o browser para o OpenRouter.
 *  2. OpenRouter redireciona de volta em {@code /auth/openrouter/callback}
 *     com {@code ?code=...}.
 *  3. {@link handleCallback}: lê o verifier salvo, troca o code pela
 *     chave final e persiste via {@link OpenAiKeyService}.
 */
@Injectable({ providedIn: 'root' })
export class OpenRouterAuthService {
  private readonly http = inject(HttpClient);
  private readonly keyService = inject(OpenAiKeyService);

  /** Dispara o login: redireciona o navegador para o OpenRouter. */
  async startLogin(): Promise<void> {
    const verifier = randomVerifier();
    const challenge = await sha256Base64Url(verifier);
    localStorage.setItem(VERIFIER_STORAGE_KEY, verifier);

    const callbackUrl = `${window.location.origin}/auth/openrouter/callback`;
    const url = new URL(AUTH_BASE);
    url.searchParams.set('callback_url', callbackUrl);
    url.searchParams.set('code_challenge', challenge);
    url.searchParams.set('code_challenge_method', 'S256');
    window.location.href = url.toString();
  }

  /** Troca o code recebido no callback pela chave de API persistente. */
  async handleCallback(code: string): Promise<void> {
    const verifier = localStorage.getItem(VERIFIER_STORAGE_KEY);
    if (!verifier) {
      throw new Error('Sessão de autenticação não encontrada. Tente novamente.');
    }
    localStorage.removeItem(VERIFIER_STORAGE_KEY);

    const body = {
      code,
      code_verifier: verifier,
      code_challenge_method: 'S256',
    };
    const resp = await firstValueFrom(
      this.http.post<{ key: string }>(TOKEN_EXCHANGE, body),
    );
    if (!resp?.key) {
      throw new Error('Resposta do OpenRouter sem chave.');
    }
    this.keyService.set(resp.key);
  }

  /** Desconecta — apaga a chave local. */
  disconnect(): void {
    this.keyService.clear();
    localStorage.removeItem(VERIFIER_STORAGE_KEY);
  }
}

// ---------------------------------------------------------------- helpers PKCE
function randomVerifier(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(64));
  return base64url(bytes);
}

async function sha256Base64Url(input: string): Promise<string> {
  const data = new TextEncoder().encode(input);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return base64url(new Uint8Array(digest));
}

function base64url(bytes: Uint8Array): string {
  let bin = '';
  for (let i = 0; i < bytes.length; i++) {
    bin += String.fromCharCode(bytes[i]);
  }
  return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
