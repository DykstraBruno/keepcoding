import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { OpenAiKeyService } from '../services/openai-key.service';

/**
 * Anexa o header {@code X-OpenAI-Key} a cada chamada do KeepCoding quando o
 * usuário tem uma chave cadastrada localmente. O backend usa essa chave para
 * construir um ChatClient da OpenAI por requisição (BYOK).
 *
 * Se não houver chave, o header não é enviado e o backend cai no fallback
 * determinístico do DevCoach / Entrevistador.
 */
export const openAiKeyInterceptor: HttpInterceptorFn = (req, next) => {
  const keyService = inject(OpenAiKeyService);
  const key = keyService.key();
  if (!key) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { 'X-OpenAI-Key': key } }));
};
