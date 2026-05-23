import { Component, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';

/** Linguagens reconhecidas pelo editor (modo de syntax do Monaco). */
export type EditorLanguage = 'java' | 'typescript';

/**
 * Editor de codigo do desafio.
 *
 * Esta versao e um MOCK funcional (textarea estilizado) para o projeto
 * rodar sem dependencias externas. Veja o template HTML para as instrucoes
 * de como plugar o Monaco real.
 */
@Component({
  selector: 'app-code-editor',
  imports: [FormsModule],
  templateUrl: './code-editor.component.html',
  styleUrl: './code-editor.component.scss',
})
export class CodeEditorComponent {
  /** Linguagem ativa - alterna o syntax highlight. */
  readonly language = input<EditorLanguage>('java');

  /** Conteudo do editor. Two-way binding: [(code)]. */
  readonly code = model<string>('');

  /** Emitido quando o usuario troca a linguagem pela toolbar. */
  readonly languageChange = output<EditorLanguage>();

  /**
   * Opcoes que seriam repassadas ao <ngx-monaco-editor>.
   * Mantidas para documentar a integracao real.
   */
  get monacoOptions(): Record<string, unknown> {
    return {
      theme: 'vs-dark',
      language: this.language(),
      automaticLayout: true,
      minimap: { enabled: false },
      fontSize: 14,
      scrollBeyondLastLine: false,
    };
  }

  selectLanguage(lang: EditorLanguage): void {
    if (lang !== this.language()) {
      this.languageChange.emit(lang);
    }
  }
}
