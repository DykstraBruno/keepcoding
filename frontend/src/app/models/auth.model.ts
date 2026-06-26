export type TierPlan = 'FREE' | 'PRO' | 'TEAM';

/** Usuário autenticado mantido no estado do app. */
export interface AuthUser {
  userId: number;
  username: string;
  email: string;
  tierPlan: TierPlan;
  xp: number;
  /** URL do avatar vinda do provider social (Google/GitHub/Apple). */
  avatarUrl?: string | null;
}
