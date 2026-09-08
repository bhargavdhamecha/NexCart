import { Component, Input } from '@angular/core';

const STATUS_CLASS: Record<string, string> = {
  CONFIRMED: 'status-ok',
  PAID: 'status-ok',
  PENDING_PAYMENT: 'status-warn',
  CREATED: 'status-warn',
  PAYMENT_FAILED: 'status-bad',
  CANCELLED: 'status-bad',
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `<span class="status" [class]="cssClass">{{ label }}</span>`,
  styles: [`
    .status {
      display: inline-flex;
      padding: 7px 10px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 850;
      white-space: nowrap;
    }
    .status-ok { background: #e9fbf4; color: #087853; }
    .status-warn { background: #fff6df; color: #9c6500; }
    .status-bad { background: #fff0f0; color: #bd2f36; }
    [data-theme="dark"] .status-ok { background: rgba(16,185,129,.15); color: var(--accent); }
    [data-theme="dark"] .status-warn { background: rgba(245,158,11,.15); color: #f5a623; }
    [data-theme="dark"] .status-bad { background: rgba(229,72,77,.15); color: var(--danger); }
  `],
})
export class StatusBadgeComponent {
  @Input({ required: true }) status!: string;

  get cssClass(): string {
    return STATUS_CLASS[this.status] ?? 'status-warn';
  }

  get label(): string {
    return this.status.replace(/_/g, ' ');
  }
}
