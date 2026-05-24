import { Routes } from '@angular/router';
import { ProblemListComponent } from './features/problem-list/problem-list.component';
import { ProblemDetailComponent } from './features/problem-detail/problem-detail.component';
import { InterviewListComponent } from './features/interview/interview-list/interview-list.component';
import { InterviewStartComponent } from './features/interview/interview-start/interview-start.component';
import { InterviewChatComponent } from './features/interview/interview-chat/interview-chat.component';
import { MatchQueueComponent } from './features/match/match-queue/match-queue.component';
import { MatchPlayComponent } from './features/match/match-play/match-play.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  { path: '', component: ProblemListComponent, canActivate: [authGuard] },
  { path: 'problems/:id', component: ProblemDetailComponent, canActivate: [authGuard] },

  { path: 'interviews', component: InterviewListComponent, canActivate: [authGuard] },
  { path: 'interviews/new', component: InterviewStartComponent, canActivate: [authGuard] },
  { path: 'interviews/:id', component: InterviewChatComponent, canActivate: [authGuard] },

  { path: 'matches', component: MatchQueueComponent, canActivate: [authGuard] },
  { path: 'matches/:id', component: MatchPlayComponent, canActivate: [authGuard] },

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
