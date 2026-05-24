/** Linha do ranking entregue pelo backend. */
export interface RankingEntry {
  position: number;
  userId: number;
  username: string;
  xp: number;
  easyCount: number;
  mediumCount: number;
  hardCount: number;
}

/** Resposta do GET /api/ranking. */
export interface RankingResponse {
  top: RankingEntry[];
  /** Posição do usuário autenticado quando ele NÃO está no top exibido. */
  me: RankingEntry | null;
  totalUsers: number;
}

/** Coluna por onde a tabela está ordenada no frontend. */
export type RankingSortKey =
  | 'position'
  | 'xp'
  | 'easyCount'
  | 'mediumCount'
  | 'hardCount';
