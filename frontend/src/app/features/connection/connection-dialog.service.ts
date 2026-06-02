import { Injectable, signal } from '@angular/core';

/**
 * Controla visibilidade do modal de conexões.
 * Componente em si vive no AppComponent.
 */
@Injectable({ providedIn: 'root' })
export class ConnectionDialogService {
  readonly visible = signal(false);

  open(): void {
    this.visible.set(true);
  }

  close(): void {
    this.visible.set(false);
  }
}
