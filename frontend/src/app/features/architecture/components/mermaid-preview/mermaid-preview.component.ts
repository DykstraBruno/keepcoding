import { Component, ElementRef, effect, input, signal, viewChild } from '@angular/core';
import mermaid from 'mermaid';

// Inicializa o Mermaid uma única vez (tema escuro, combina com o app).
mermaid.initialize({ startOnLoad: false, theme: 'dark' });

/** Contador para gerar ids únicos a cada render. */
let renderSeq = 0;

/** Renderiza um diagrama Mermaid a partir do código recebido. */
@Component({
  selector: 'app-mermaid-preview',
  imports: [],
  template: `
    <div class="mermaid-preview">
      @if (errorMsg()) {
        <pre class="mermaid-error">{{ errorMsg() }}</pre>
      }
      <div #host class="mermaid-host" [class.mermaid-host--hidden]="errorMsg()"></div>
    </div>
  `,
  styles: [
    `
      .mermaid-preview {
        background: var(--panel-2);
        border: 1px solid var(--border);
        border-radius: 8px;
        padding: 1rem;
        min-height: 180px;
        overflow: auto;
      }
      .mermaid-host ::ng-deep svg {
        max-width: 100%;
        height: auto;
      }
      .mermaid-host--hidden {
        display: none;
      }
      .mermaid-error {
        margin: 0;
        color: #ff8a80;
        font-size: 0.8rem;
        white-space: pre-wrap;
      }
    `,
  ],
})
export class MermaidPreviewComponent {
  /** Código-fonte do diagrama em sintaxe Mermaid. */
  readonly code = input<string>('');

  private readonly host = viewChild<ElementRef<HTMLDivElement>>('host');

  /** Mensagem de erro de sintaxe (null = diagrama válido). */
  readonly errorMsg = signal<string | null>(null);

  constructor() {
    // Re-renderiza sempre que o código (ou o host) mudar.
    effect(() => {
      const code = this.code();
      const host = this.host()?.nativeElement;
      if (host) {
        void this.renderDiagram(code, host);
      }
    });
  }

  private async renderDiagram(code: string, host: HTMLDivElement): Promise<void> {
    if (!code.trim()) {
      host.innerHTML = '';
      this.errorMsg.set(null);
      return;
    }
    try {
      const { svg } = await mermaid.render(`mermaid-${renderSeq++}`, code);
      host.innerHTML = svg;
      this.errorMsg.set(null);
    } catch (e) {
      this.errorMsg.set('Diagrama inválido:\n' + (e instanceof Error ? e.message : String(e)));
    }
  }
}
