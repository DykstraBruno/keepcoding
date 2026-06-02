import { Injectable, inject } from '@angular/core';
import { OAuthPopupResult, OAuthProvider } from '../models/connection.model';
import { ConnectionService } from './connection.service';

/**
 * Abre o popup OAuth do provider, escuta o resultado via postMessage
 * (vindo da página de callback do front), valida origem e fecha.
 *
 * Fluxo:
 *  1. Pede /start ao backend → URL de authorize + state
 *  2. Abre popup centralizado
 *  3. Espera evento 'oauth:result' OU detecta popup fechado
 *  4. Refresca conexões antes de resolver
 */
@Injectable({ providedIn: 'root' })
export class OAuthPopupService {
  private readonly connections = inject(ConnectionService);

  async openProviderLogin(provider: OAuthProvider): Promise<OAuthPopupResult> {
    const start = await this.connections.start(provider);

    // Popup centralizado.
    const w = 520;
    const h = 640;
    const left = window.screenX + (window.outerWidth - w) / 2;
    const top = window.screenY + (window.outerHeight - h) / 2;
    const features = `width=${w},height=${h},left=${left},top=${top},resizable=yes,scrollbars=yes`;
    const popup = window.open(start.authorizeUrl, `oauth_${provider}`, features);
    if (!popup) {
      throw new Error('Popup bloqueado pelo navegador. Permita popups para este site.');
    }

    const trustedOrigin = window.location.origin;

    return new Promise<OAuthPopupResult>((resolve) => {
      const onMessage = async (event: MessageEvent) => {
        // CRÍTICO: nunca confia em mensagens de outras origens.
        if (event.origin !== trustedOrigin) {
          return;
        }
        if (event.data?.type !== 'oauth:result') {
          return;
        }
        cleanup();
        try {
          popup.close();
        } catch {
          /* ignore */
        }
        const payload = event.data.payload as OAuthPopupResult;
        if (payload.status === 'success') {
          await this.connections.refresh();
        }
        resolve(payload);
      };
      window.addEventListener('message', onMessage);

      // Polling pra detectar fechamento manual do popup.
      const interval = window.setInterval(() => {
        if (popup.closed) {
          cleanup();
          resolve({ status: 'closed', provider });
        }
      }, 500);

      function cleanup(): void {
        window.removeEventListener('message', onMessage);
        window.clearInterval(interval);
      }
    });
  }
}
