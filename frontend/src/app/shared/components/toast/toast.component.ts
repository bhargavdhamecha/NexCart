import { Component, inject } from '@angular/core';
import { ToastService } from '../../../core/services/toast.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (toastService.toast(); as t) {
      <div class="toast show" [class]="'toast show toast--' + t.type">
        {{ t.message }}
      </div>
    } @else {
      <div class="toast"></div>
    }
  `,
  styles: [`
    .toast {
      position: fixed;
      right: 22px;
      bottom: 22px;
      background: #111827;
      color: white;
      padding: 14px 18px;
      border-radius: 14px;
      box-shadow: var(--shadow);
      opacity: 0;
      transform: translateY(14px);
      pointer-events: none;
      transition: .25s;
      z-index: 9999;
      font-weight: 600;
      font-size: 14px;
      max-width: 320px;
    }
    .toast.show {
      opacity: 1;
      transform: translateY(0);
      pointer-events: auto;
    }
    .toast--success { border-left: 4px solid var(--accent); }
    .toast--error   { border-left: 4px solid var(--danger); }
    .toast--info    { border-left: 4px solid var(--brand); }
  `],
})
export class ToastComponent {
  toastService = inject(ToastService);
}
