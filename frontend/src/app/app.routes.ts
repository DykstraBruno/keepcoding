import { Routes } from '@angular/router';
import { ProblemListComponent } from './features/problem-list/problem-list.component';
import { ProblemDetailComponent } from './features/problem-detail/problem-detail.component';
import { InterviewListComponent } from './features/interview/interview-list/interview-list.component';
import { InterviewStartComponent } from './features/interview/interview-start/interview-start.component';
import { InterviewChatComponent } from './features/interview/interview-chat/interview-chat.component';
import { MatchQueueComponent } from './features/match/match-queue/match-queue.component';
import { MatchPlayComponent } from './features/match/match-play/match-play.component';
import { RankingComponent } from './features/ranking/ranking.component';
import { LoginComponent } from './features/auth/login/login.component';
import { AuthCallbackComponent } from './features/auth/auth-callback/auth-callback.component';
import { OAuthCallbackComponent } from './features/connection/oauth-callback.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  // Login social (Supabase): Google / Apple / GitHub.
  { path: 'login', component: LoginComponent },
  // Retorno do redirectTo do login social — materializa a sessão e vai à dashboard.
  { path: 'auth/callback', component: AuthCallbackComponent },

  // Callback do popup OAuth da IA (Google/etc.), distinto do login do app.
  // Roda dentro do popup, faz o exchange via JWT e fecha via postMessage.
  { path: 'oauth/callback', component: OAuthCallbackComponent, canActivate: [authGuard] },

  { path: '', component: ProblemListComponent, canActivate: [authGuard] },
  { path: 'problems/:id', component: ProblemDetailComponent, canActivate: [authGuard] },

  { path: 'interviews', component: InterviewListComponent, canActivate: [authGuard] },
  { path: 'interviews/new', component: InterviewStartComponent, canActivate: [authGuard] },
  { path: 'interviews/:id', component: InterviewChatComponent, canActivate: [authGuard] },

  { path: 'matches', component: MatchQueueComponent, canActivate: [authGuard] },
  { path: 'matches/:id', component: MatchPlayComponent, canActivate: [authGuard] },

  { path: 'ranking', component: RankingComponent, canActivate: [authGuard] },

  // Pública: política de privacidade LGPD.
  {
    path: 'privacidade',
    loadComponent: () =>
      import('./features/privacy/privacy-policy.component').then(
        (m) => m.PrivacyPolicyComponent,
      ),
  },

  // Lazy-load: a feature de arquitetura carrega o Mermaid (dependência pesada)
  // só quando o usuário acessa estas rotas, mantendo o bundle inicial enxuto.
  {
    path: 'architecture',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/architecture/architecture-list/architecture-list.component').then(
        (m) => m.ArchitectureListComponent,
      ),
  },
  {
    path: 'architecture/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import(
        './features/architecture/architecture-challenge/architecture-challenge.component'
      ).then((m) => m.ArchitectureChallengeComponent),
  },

  { path: '**', redirectTo: '' },
];
