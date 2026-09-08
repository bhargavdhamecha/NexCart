import { Component, Input } from '@angular/core';

export type OrderFlowStep = 'cart' | 'review' | 'payment' | 'confirmed';

const STEPS: { key: OrderFlowStep; label: string }[] = [
  { key: 'cart', label: 'Cart' },
  { key: 'review', label: 'Review' },
  { key: 'payment', label: 'Payment' },
  { key: 'confirmed', label: 'Confirmed' },
];

@Component({
  selector: 'app-order-stepper',
  standalone: true,
  template: `
    <div class="stepper">
      @for (step of steps; track step.key; let i = $index) {
        <div class="step" [class.active]="i === currentIndex" [class.done]="i < currentIndex">
          <span class="dot">{{ i < currentIndex ? '✓' : i + 1 }}</span>
          <span class="label">{{ step.label }}</span>
        </div>
        @if (i < steps.length - 1) {
          <span class="connector" [class.done]="i < currentIndex"></span>
        }
      }
    </div>
  `,
  styles: [`
    .stepper {
      display: flex;
      align-items: center;
      margin: 14px 0 22px;
    }
    .step {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .dot {
      width: 26px;
      height: 26px;
      border-radius: 999px;
      display: grid;
      place-items: center;
      font-size: 12px;
      font-weight: 800;
      background: var(--surface-2);
      color: var(--muted);
      border: 1px solid var(--line);
      flex-shrink: 0;
    }
    .step.active .dot, .step.done .dot {
      background: linear-gradient(135deg, var(--brand), var(--brand-2));
      color: #fff;
      border-color: transparent;
    }
    .label {
      font-size: 13px;
      font-weight: 700;
      color: var(--muted);
    }
    .step.active .label, .step.done .label { color: var(--text); }
    .connector {
      flex: 1;
      height: 2px;
      background: var(--line);
      margin: 0 10px;
      max-width: 60px;
    }
    .connector.done {
      background: linear-gradient(90deg, var(--brand), var(--brand-2));
    }
    @media(max-width:680px) {
      .label { display: none; }
      .connector { max-width: 24px; }
    }
  `],
})
export class OrderStepperComponent {
  @Input({ required: true }) currentStep!: OrderFlowStep;
  steps = STEPS;

  get currentIndex(): number {
    return this.steps.findIndex((s) => s.key === this.currentStep);
  }
}
