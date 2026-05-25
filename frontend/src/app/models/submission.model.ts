import { CoachFeedback } from './coach.model';
import type { ApiLanguage } from '../data/languages.catalog';

/** Identificador da linguagem enviada ao backend (derivado do catálogo). */
export type Language = ApiLanguage;

export type SubmissionStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'WRONG_ANSWER'
  | 'TIME_LIMIT'
  | 'ERROR';

/** Payload do POST /api/submissions. O usuário vem do token JWT. */
export interface SubmissionRequest {
  problemId: number;
  language: Language;
  code: string;
}

/** Resposta do POST /api/submissions. */
export interface SubmissionResponse {
  submissionId: number;
  status: SubmissionStatus;
  passedTests: number;
  totalTests: number;
  coachFeedback: CoachFeedback;
  createdAt: string;
  /** XP creditado nesta submissão (0 se não houve primeiro acerto). */
  xpAwarded: number;
  /** true se esta foi a primeira vez que o usuário resolveu o problema. */
  firstSolve: boolean;
  /** XP total do usuário após esta submissão. */
  userXp: number;
}
