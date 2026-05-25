import { Difficulty, Problem } from './problem.model';
import { Language, SubmissionStatus } from './submission.model';

export type MatchStatus = 'ACTIVE' | 'COMPLETED' | 'ABANDONED';

export type MatchEventType =
  | 'MATCH_FOUND'
  | 'SUBMISSION'
  | 'WIN'
  | 'FORFEIT'
  | 'MATCH_ENDED';

/** Representação enxuta de um jogador (espelha PlayerView do backend). */
export interface PlayerView {
  id: number;
  username: string;
}

/** Status entregue ao próprio usuário enquanto na fila. */
export interface QueueStatus {
  state: 'WAITING' | 'LEFT' | 'ERROR';
  difficulty: Difficulty | null;
  message: string;
}

/** Estado completo de uma partida. */
export interface MatchState {
  matchId: number;
  difficulty: Difficulty;
  status: MatchStatus;
  player1: PlayerView;
  player2: PlayerView;
  winner: PlayerView | null;
  forfeitReason: string | null;
  problem: Problem;
  startedAt: string;
  endedAt: string | null;
}

/** Evento publicado em /topic/match/{id}. */
export interface MatchEvent {
  type: MatchEventType;
  actor: PlayerView | null;
  result: SubmissionStatus | null;
  passedTests: number | null;
  totalTests: number | null;
  winner: PlayerView | null;
  forfeitReason: string | null;
  timestamp: string;
}

/** Payload de POST /api/matches/{id}/submit. */
export interface MatchSubmitRequest {
  language: Language;
  code: string;
}
