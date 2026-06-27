import { Injectable, computed, signal } from '@angular/core';
import {
  ConsentCategory,
  ConsentRecord,
  defaultConsent,
} from '../models/consent.model';

const STORAGE_KEY = 'keepcoding.lgpd-consent.v1';

/**
 * Estado de consentimento LGPD persistido em localStorage.
 *
 * <p>O signal {@link record} é a fonte da verdade pra UI; {@link decided}
 * controla a exibição do banner inicial. Mudanças disparam um evento
 * `consentchange` no `window` pra integradores opcionais (analytics
 * lazy-load, por ex.) reagirem.</p>
 */
@Injectable({ providedIn: 'root' })
export class ConsentService {
  private readonly _record = signal<ConsentRecord | null>(this.load());
  readonly record = this._record.asReadonly();

  /** True se o usuário já tomou uma decisão (banner não precisa mais aparecer). */
  readonly decided = computed(() => this._record() !== null);

  allows(category: ConsentCategory): boolean {
    if (category === 'necessary') return true;
    const r = this._record();
    return r ? !!r[category] : false;
  }

  acceptAll(): void {
    this.save({
      ...defaultConsent(),
      preferences: true,
      analytics: true,
      marketing: true,
    });
  }

  rejectNonEssential(): void {
    this.save(defaultConsent());
  }

  saveCustom(opts: { preferences: boolean; analytics: boolean; marketing: boolean }): void {
    this.save({
      ...defaultConsent(),
      preferences: opts.preferences,
      analytics: opts.analytics,
      marketing: opts.marketing,
    });
  }

  /** Apaga a decisão; banner reaparece. Usado pelo botão "Revogar consentimento". */
  reset(): void {
    localStorage.removeItem(STORAGE_KEY);
    this._record.set(null);
    this.broadcast(null);
  }

  private save(record: ConsentRecord): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(record));
    this._record.set(record);
    this.broadcast(record);
  }

  private load(): ConsentRecord | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw) as ConsentRecord;
      return parsed?.version === 1 ? parsed : null;
    } catch {
      return null;
    }
  }

  private broadcast(record: ConsentRecord | null): void {
    window.dispatchEvent(new CustomEvent('consentchange', { detail: record }));
  }
}
