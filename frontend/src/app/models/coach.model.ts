/** Classificação de qualidade atribuída pelo DevCoach. */
export type CoachClassification =
  | 'Brilhante'
  | 'Ótimo'
  | 'Livro'
  | 'Incompleto'
  | 'Gafe';

/**
 * Feedback de CÓDIGO retornado pela API (POST /api/submissions).
 * Espelha o record CoachFeedback do backend.
 */
export interface CoachFeedback {
  classification: CoachClassification;
  headline: string;
  socratic_hint: string;
  time_complexity: string;
  space_complexity: string;
  score: number;
}

/** Métrica genérica exibida no painel do DevCoach (rótulo + valor). */
export interface CoachMetric {
  label: string;
  value: string;
}

/**
 * View-model genérico do painel DevCoach.
 * Tanto o feedback de código quanto o de arquitetura são mapeados para cá.
 */
export interface CoachVerdict {
  classification: CoachClassification;
  headline: string;
  hint: string;
  metrics: CoachMetric[];
  score: number;
}
