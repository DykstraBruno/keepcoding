import {
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
  output,
  signal,
} from '@angular/core';
import { environment } from '../../../environments/environment';

/**
 * Wrapper Angular para o widget Cloudflare Turnstile (CAPTCHA).
 *
 * <p>Carrega o script da Cloudflare sob demanda (idempotente), renderiza o
 * widget no container e emite o token via {@link tokenChange}. Token expira
 * em ~300s — quando isso acontece, emite {@code null} pra o pai bloquear o
 * submit. Em erro de rede/bloqueio, expõe {@link error} pra UI explicar.</p>
 *
 * <p>O <em>secret</em> NUNCA vive no front; só o site key. A validação real
 * do token acontece no Supabase Auth (Bot and Abuse Protection) usando a
 * secret configurada no painel.</p>
 */
@Component({
  selector: 'app-turnstile',
  imports: [],
  template: `
    <div #host class="turnstile" aria-label="Verificação anti-bot"></div>
    @if (error()) {
      <p class="turnstile__error" role="alert">{{ error() }}</p>
    }
  `,
  styles: [
    `
      .turnstile {
        display: flex;
        justify-content: center;
        min-height: 65px;
      }
      .turnstile__error {
        margin: 0.4rem 0 0;
        font-size: 0.78rem;
        color: var(--hard);
        text-align: center;
      }
    `,
  ],
})
export class TurnstileComponent implements OnInit {
  @ViewChild('host', { static: true })
  private host!: ElementRef<HTMLElement>;

  /** Emite o token (string) quando validado, ou null se expirar/falhar. */
  readonly tokenChange = output<string | null>();

  readonly error = signal<string | null>(null);

  private widgetId: string | null = null;
  private readonly destroyRef = inject(DestroyRef);

  async ngOnInit(): Promise<void> {
    try {
      await loadTurnstileScript();
      this.render();
    } catch (e) {
      const msg =
        e instanceof Error
          ? e.message
          : 'Não foi possível carregar a verificação de segurança.';
      this.error.set(msg);
      this.tokenChange.emit(null);
    }

    this.destroyRef.onDestroy(() => this.cleanup());
  }

  private render(): void {
    const turnstile = (window as TurnstileWindow).turnstile;
    if (!turnstile) {
      this.error.set('Verificação de segurança indisponível.');
      this.tokenChange.emit(null);
      return;
    }
    this.widgetId = turnstile.render(this.host.nativeElement, {
      sitekey: environment.captcha.turnstileSiteKey,
      theme: 'dark',
      callback: (token: string) => {
        this.error.set(null);
        this.tokenChange.emit(token);
      },
      'expired-callback': () => {
        this.tokenChange.emit(null);
      },
      'error-callback': () => {
        this.error.set('Verificação falhou. Recarregue a página.');
        this.tokenChange.emit(null);
      },
    });
  }

  private cleanup(): void {
    const turnstile = (window as TurnstileWindow).turnstile;
    if (turnstile && this.widgetId) {
      try {
        turnstile.remove(this.widgetId);
      } catch {
        /* widget já removido — sem ação */
      }
    }
  }
}

// ---------------------------------------------------------------- bootstrap

interface TurnstileApi {
  render(
    container: HTMLElement,
    options: {
      sitekey: string;
      theme?: 'light' | 'dark' | 'auto';
      callback?: (token: string) => void;
      'expired-callback'?: () => void;
      'error-callback'?: () => void;
    },
  ): string;
  remove(widgetId: string): void;
  reset(widgetId?: string): void;
}

interface TurnstileWindow extends Window {
  turnstile?: TurnstileApi;
}

let scriptPromise: Promise<void> | null = null;

/** Carrega o script da Cloudflare uma única vez por sessão de browser. */
function loadTurnstileScript(): Promise<void> {
  if (scriptPromise) {
    return scriptPromise;
  }
  scriptPromise = new Promise((resolve, reject) => {
    if ((window as TurnstileWindow).turnstile) {
      resolve();
      return;
    }
    const script = document.createElement('script');
    script.src =
      'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () =>
      reject(new Error('Falha ao carregar verificação anti-bot.'));
    document.head.appendChild(script);
  });
  return scriptPromise;
}
