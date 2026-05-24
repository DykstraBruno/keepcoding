export type InterviewStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED';

export type MessageRole = 'INTERVIEWER' | 'CANDIDATE';

/**
 * Fase atual da entrevista (formato padrão: 10 min apresentação + 30 min perguntas).
 */
export type InterviewPhase = 'PRESENTATION' | 'QUESTIONS' | 'COMPLETED';

/** Feedback final estruturado da IA (espelha o backend). */
export interface InterviewFeedback {
  classification: string;
  summary: string;
  strengths: string[];
  gaps: string[];
  suggestions: string[];
  score: number;
}

/** Resposta de POST /interviews/start e POST /interviews/{id}/answer. */
export interface InterviewTurnResponse {
  interviewId: number;
  status: InterviewStatus;
  phase: InterviewPhase;
  questionNumber: number;
  totalQuestions: number;
  nextQuestion: string | null;
  finished: boolean;
  feedback: InterviewFeedback | null;
}

/** Item de listagem do histórico de entrevistas. */
export interface InterviewSummary {
  id: number;
  targetRole: string;
  status: InterviewStatus;
  questionsAnswered: number;
  totalQuestions: number;
  createdAt: string;
  finishedAt: string | null;
}

/** Mensagem individual do diálogo. */
export interface InterviewMessageView {
  role: MessageRole;
  turnIndex: number;
  content: string;
  createdAt: string;
}

/** Detalhe completo de uma entrevista (contexto + transcrição + feedback). */
export interface InterviewDetail {
  id: number;
  targetRole: string;
  resumeText: string;
  status: InterviewStatus;
  phase: InterviewPhase;
  maxQuestions: number;
  messages: InterviewMessageView[];
  feedback: InterviewFeedback | null;
  createdAt: string;
  finishedAt: string | null;
}

/** Payload de POST /interviews/start. */
export interface StartInterviewRequest {
  targetRole: string;
  resumeText: string;
  maxQuestions?: number;
}

/** Payload de POST /interviews/{id}/answer. */
export interface AnswerRequest {
  content: string;
}
