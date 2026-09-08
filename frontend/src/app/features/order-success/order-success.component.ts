import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { InrCurrencyPipe } from '../../shared/pipes/inr-currency.pipe';
import { Order } from '../../core/models/order.model';
import { OrderStepperComponent } from '../../shared/components/order-stepper/order-stepper.component';

@Component({
  selector: 'app-order-success',
  standalone: true,
  imports: [RouterLink, InrCurrencyPipe, OrderStepperComponent],
  template: `
    <div class="container page-wrap">
      <app-order-stepper currentStep="confirmed" />
      @if (order(); as o) {
        <div class="panel success-box center">
          <div class="success-icon">✓</div>
          <h2>Order confirmed!</h2>
          <p class="small">
            Your order <strong>#{{ o.orderId }}</strong> has been created successfully.
          </p>
          <div class="success-amount">{{ o.totalAmount | inrCurrency }}</div>

          <div class="items-recap">
            @for (item of o.items; track item.orderItemId) {
              <div class="recap-item">
                <div class="recap-img">
                  @if (item.imageUrl) {
                    <img [src]="item.imageUrl" [alt]="item.title">
                  } @else {
                    📦
                  }
                </div>
                <div class="recap-info">
                  <strong>{{ item.title }}</strong>
                  <div class="small">Qty {{ item.quantity }} × {{ item.unitPrice | inrCurrency }}</div>
                </div>
              </div>
            }
          </div>

          <div class="cta-row">
            <a routerLink="/orders" class="primary-btn" id="view-orders-btn">View my orders</a>
            <a routerLink="/products" class="secondary-btn" id="continue-shopping-btn">Continue shopping</a>
          </div>
        </div>
      } @else if (loading()) {
        <div class="panel success-box center">
          <div class="loading-spinner">⌛</div>
          <p>Loading order...</p>
        </div>
      } @else {
        <div class="panel success-box center">
          <h2>Order not found</h2>
          <div class="cta-row">
            <a routerLink="/orders" class="primary-btn" id="view-orders-btn">View my orders</a>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .success-box {
      max-width: 620px;
      margin: 0 auto;
      padding: 48px 28px;
    }
    .center { text-align: center; }
    .success-icon {
      width: 90px;
      height: 90px;
      border-radius: 999px;
      background: #e9fbf4;
      color: #087853;
      display: grid;
      place-items: center;
      font-size: 44px;
      margin: 8px auto 18px;
    }
    [data-theme="dark"] .success-icon { background: rgba(16,185,129,.15); color: var(--accent); }
    h2 { color: var(--text); }
    .success-amount {
      font-size: 34px;
      font-weight: 900;
      color: var(--text);
      margin: 12px 0 24px;
    }
    .items-recap {
      text-align: left;
      border-top: 1px solid var(--line);
      border-bottom: 1px solid var(--line);
      margin-bottom: 26px;
    }
    .recap-item {
      display: grid;
      grid-template-columns: 56px 1fr;
      gap: 14px;
      align-items: center;
      padding: 14px 0;
      border-bottom: 1px solid var(--line);
    }
    .recap-item:last-child { border-bottom: 0; }
    .recap-img {
      height: 56px;
      border-radius: 12px;
      background: linear-gradient(145deg, #f0f4ff, #ece8ff);
      display: grid;
      place-items: center;
      font-size: 26px;
      overflow: hidden;
    }
    .recap-img img { width: 100%; height: 100%; object-fit: cover; }
    [data-theme="dark"] .recap-img { background: linear-gradient(145deg, #1e2240, #241b40); }
    .recap-info strong { color: var(--text); }
    .cta-row {
      display: flex;
      justify-content: center;
      gap: 12px;
      flex-wrap: wrap;
    }
    a { text-decoration: none; }
    .loading-spinner { font-size: 40px; margin-bottom: 10px; }
  `],
})
export class OrderSuccessComponent implements OnInit {
  private orderService = inject(OrderService);
  private route = inject(ActivatedRoute);

  order = signal<Order | undefined>(undefined);
  loading = signal(true);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.orderService.getOrderById(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
