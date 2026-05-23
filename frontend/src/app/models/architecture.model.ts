import { Difficulty } from './problem.model';
import { CoachClassification } from './coach.model';

/** Resumo de um desafio de arquitetura para listagem. */
export interface ArchitectureChallengeSummary {
  id: number;
  title: string;
  difficulty: Difficulty;
}

/** Detalhe de um desafio de arquitetura. */
export interface ArchitectureChallenge {
  id: number;
  title: string;
  context: string;
  requirements: string;
  difficulty: Difficulty;
}

/** Feedback do DevCoach na análise de arquitetura (espelha o backend). */
export interface ArchitectFeedback {
  classification: CoachClassification;
  headline: string;
  socratic_hint: string;
  scalability: string;
  resilience: string;
  score: number;
}

/** Payload do POST /api/architecture/submissions. */
export interface ArchitectureSubmissionRequest {
  challengeId: number;
  mermaidCode: string;
  notes: string;
}

/** Resposta do POST /api/architecture/submissions. */
export interface ArchitectureSubmissionResponse {
  submissionId: number;
  coachFeedback: ArchitectFeedback;
  createdAt: string;
}
