import { Component, computed, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  EditorLanguage,
  LANGUAGE_CATALOG,
  metaFor,
} from '../../../../data/languages.catalog';

// Re-export para os componentes externos que usavam o tipo a partir daqui.
export type { EditorLanguage } from '../../../../data/languages.catalog';

/**
 * Editor de código do desafio.
 *
 * Toolbar com dropdown de linguagem (catálogo central) e botão
 * "📖 Docs" abrindo a documentação oficial da linguagem ativa em
 * uma nova aba.
 *
 * Esta versão segue sendo um MOCK funcional (textarea estilizado);
 * pra plugar Monaco real, ver instruções no HTML.
 */
@Component({
  selector: 'app-code-editor',
  imports: [FormsModule],
  templateUrl: './code-editor.component.html',
  styleUrl: './code-editor.component.scss',
})
export class CodeEditorComponent {
  /** Linguagem ativa. */
  readonly language = input<EditorLanguage>('java');

  /** Conteúdo do editor. Two-way binding via [(code)]. */
  readonly code = model<string>('');

  /** Emitido quando o usuário troca a linguagem pelo dropdown. */
  readonly languageChange = output<EditorLanguage>();

  /** Catálogo exposto pro template iterar e popular o select. */
  readonly catalog = LANGUAGE_CATALOG;

  /** Metadados da linguagem ativa (label, docsUrl, etc.). */
  readonly currentMeta = computed(() => metaFor(this.language()));

  /**
   * Opções repassadas ao {@code <ngx-monaco-editor>} quando o Monaco
   * real for plugado (ver template).
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

  onLanguageSelect(id: EditorLanguage): void {
    if (id !== this.language()) {
      this.languageChange.emit(id);
    }
  }
}
