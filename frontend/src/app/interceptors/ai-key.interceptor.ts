import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AiKeyService } from '../services/ai-key.service';

/** Rotas que efetivamente chamam IA no backend. */
const AI_PATH_PATTERNS: readonly RegExp[] = [
  /\/api\/submissions/,
  /\/api\/interviews/,
  /\/api\/architecture/,
];

/**
 * Anexa `X-AI-Provider` + `X-AI-Key` (BYOK) só nas rotas que vão usar IA.
 * Não vaza a chave em outras chamadas (auth, ranking, problemas, etc.).
 */
export const aiKeyInterceptor: HttpInterceptorFn = (req, next) => {
  const aiKey = inject(AiKeyService);
  const cfg = aiKey.config();
  if (!cfg) {
    return next(req);
  }
  if (!AI_PATH_PATTERNS.some((re) => re.test(req.url))) {
    return next(req);
  }
  const enriched = req.clone({
    setHeaders: {
      'X-AI-Provider': cfg.provider,
      'X-AI-Key': cfg.apiKey,
    },
  });
  return next(enriched);
};
