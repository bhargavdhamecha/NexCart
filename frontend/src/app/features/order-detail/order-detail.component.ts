import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { InrCurrencyPipe } from '../../shared/pipes/inr-currency.pipe';
import { Order } from '../../core/models/order.model';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [RouterLink, InrCurrencyPipe, StatusBadgeComponent],
  template: `
    <div class="container page-wrap">
      <div class="breadcrumb">
        <a routerLink="/orders">My orders</a> / <span>Order details</span>
      </div>

      @if (order(); as o) {
        <div class="section-head">
          <div>
            <h2>Order #{{ o.orderId }}</h2>
            <p>{{ formatDate(o.orderDate) }}</p>
          </div>
          <app-status-badge [status]="o.status" />
        </div>

        <div class="panel item-list">
          @for (item of o.items; track item.orderItemId) {
            <div class="order-item">
              <div class="item-img">
                @if (item.imageUrl) {
                  <img [src]="item.imageUrl" [alt]="item.title">
                } @else {
                  📦
                }
              </div>
              <div>
                <strong>{{ item.title }}</strong>
                <div class="small" style="margin-top:5px">Qty {{ item.quantity }} × {{ item.unitPrice | inrCurrency }}</div>
              </div>
              <strong>{{ item.itemTotal | inrCurrency }}</strong>
            </div>
          }
          <div class="order-total">
            <span>Total</span>
            <strong>{{ o.totalAmount | inrCurrency }}</strong>
          </div>
        </div>
      } @else if (loading()) {
        <div class="loading-state">
          <p>Loading order...</p>
        </div>
      } @else {
        <div class="error-state">
          <h2>Order not found</h2>
          <a routerLink="/orders" class="primary-btn">Back to my orders</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .section-head {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 20px;
    }
    .section-head h2 { margin: 0; color: var(--text); }
    .section-head p { margin: 4px 0 0; color: var(--muted); }
    .item-list { padding: 6px 20px; }
    .order-item {
      display: grid;
      grid-template-columns: 70px 1fr auto;
      gap: 16px;
      align-items: center;
      padding: 18px 0;
      border-bottom: 1px solid var(--line);
    }
    .order-item strong { color: var(--text); }
    .item-img {
      height: 64px;
      border-radius: 12px;
      background: linear-gradient(145deg, #f0f4ff, #ece8ff);
      display: grid;
      place-items: center;
      font-size: 28px;
      overflow: hidden;
    }
    .item-img img { width: 100%; height: 100%; object-fit: cover; }
    [data-theme="dark"] .item-img { background: linear-gradient(145deg, #1e2240, #241b40); }
    .order-total {
      display: flex;
      justify-content: space-between;
      font-size: 18px;
      font-weight: 900;
      padding-top: 18px;
      margin-top: 4px;
      color: var(--text);
    }
    .loading-state, .error-state {
      text-align: center;
      padding: 60px 20px;
      color: var(--muted);
    }
  `],
})
export class OrderDetailComponent implements OnInit {
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

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  }
}
