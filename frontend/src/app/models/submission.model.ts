import { CoachFeedback } from './coach.model';

export type Language = 'JAVA' | 'TYPESCRIPT';

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
}
