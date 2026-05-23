import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

/** Tela de criação de conta. */
@Component({
  selector: 'app-register',
  imports: [FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly username = signal('');
  readonly email = signal('');
  readonly password = signal('');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  submit(): void {
    if (this.loading()) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    this.auth
      .register({
        username: this.username(),
        email: this.email(),
        password: this.password(),
      })
      .subscribe({
        next: () => this.router.navigate(['/']),
        error: (err) => {
          this.error.set(err?.error?.error ?? 'Não foi possível criar a conta.');
          this.loading.set(false);
        },
      });
  }
}
