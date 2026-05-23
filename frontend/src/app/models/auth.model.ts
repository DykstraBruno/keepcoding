export type TierPlan = 'FREE' | 'PRO' | 'TEAM';

/** Payload do POST /api/auth/register. */
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

/** Payload do POST /api/auth/login. */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Resposta do backend: token + dados do usuário. */
export interface AuthResponse {
  token: string;
  userId: number;
  username: string;
  email: string;
  tierPlan: TierPlan;
  xp: number;
}

/** Usuário autenticado mantido no estado do app. */
export interface AuthUser {
  userId: number;
  username: string;
  email: string;
  tierPlan: TierPlan;
  xp: number;
}
