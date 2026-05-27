import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { OpenAiKeyDialogService } from '../../services/openai-key-dialog.service';
import { OpenAiKeyService } from '../../services/openai-key.service';

/**
 * Modal de cadastro/atualização da chave OpenAI do usuário (BYOK).
 *
 * Renderizado no shell do AppComponent. Abre quando
 * {@link OpenAiKeyDialogService#visible} fica true — ou via
 * {@link OpenAiKeyDialogService#requestKey} (que retorna uma Promise
 * pra quem disparou a ação de IA).
 */
@Component({
  selector: 'app-openai-key-dialog',
  imports: [FormsModule],
  templateUrl: './openai-key-dialog.component.html',
  styleUrl: './openai-key-dialog.component.scss',
})
export class OpenAiKeyDialogComponent {
  readonly dialog = inject(OpenAiKeyDialogService);
  readonly keyService = inject(OpenAiKeyService);

  readonly draft = signal('');
  readonly showKey = signal(false);

  onSave(): void {
    const value = this.draft().trim();
    if (!value) {
      return;
    }
    this.dialog.resolveSaved(value);
    this.draft.set('');
    this.showKey.set(false);
  }

  onCancel(): void {
    this.dialog.resolveCancelled();
    this.draft.set('');
    this.showKey.set(false);
  }

  onRemove(): void {
    this.keyService.clear();
    this.draft.set('');
    this.showKey.set(false);
  }

  toggleShowKey(): void {
    this.showKey.update((v) => !v);
  }
}
