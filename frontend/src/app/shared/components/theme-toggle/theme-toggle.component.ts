import { Component, inject } from '@angular/core';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  template: `
    <button
      class="icon-btn theme-toggle"
      (click)="themeService.toggle()"
      [attr.aria-label]="themeService.theme() === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'"
      title="Toggle theme"
    >
      @if (themeService.theme() === 'dark') {
        ☀️
      } @else {
        🌙
      }
    </button>
  `,
  styles: [`
    .theme-toggle {
      font-size: 18px;
    }
  `],
})
export class ThemeToggleComponent {
  themeService = inject(ThemeService);
}
