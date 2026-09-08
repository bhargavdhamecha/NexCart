import { Injectable, signal, effect } from '@angular/core';

export type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  /** Resolved theme: reads localStorage first, then OS preference */
  theme = signal<Theme>(this.resolveInitialTheme());

  constructor() {
    // Sync data-theme attribute on document whenever signal changes
    effect(() => {
      document.documentElement.setAttribute('data-theme', this.theme());
      localStorage.setItem('nex_theme', this.theme());
    });

    // Listen for OS-level theme changes (only if user hasn't manually set a preference)
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    mq.addEventListener('change', (e) => {
      if (!localStorage.getItem('nex_theme')) {
        this.theme.set(e.matches ? 'dark' : 'light');
      }
    });
  }

  toggle(): void {
    this.theme.set(this.theme() === 'dark' ? 'light' : 'dark');
  }

  private resolveInitialTheme(): Theme {
    const stored = localStorage.getItem('nex_theme') as Theme | null;
    if (stored === 'dark' || stored === 'light') return stored;
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
}
