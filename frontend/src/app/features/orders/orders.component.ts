import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { InrCurrencyPipe } from '../../shared/pipes/inr-currency.pipe';
import { Order } from '../../core/models/order.model';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [RouterLink, InrCurrencyPipe, StatusBadgeComponent],
  template: `
    <div class="container page-wrap">
      <div class="breadcrumb">Home / My Orders</div>
      <div class="section-head">
        <div>
          <h2>My orders</h2>
          <p>Track current and previous purchases.</p>
        </div>
      </div>

      @if (loading()) {
        <div class="panel loading-state">
          <div class="loading-spinner">⌛</div>
          <p>Loading your orders...</p>
        </div>
      } @else {
        @for (order of orders(); track order.orderId) {
          <div class="panel order-card">
            <div>
              <strong>Order #{{ order.orderId }}</strong>
              <div class="order-meta">
                {{ formatDate(order.orderDate) }} • {{ order.items.length }} item{{ order.items.length !== 1 ? 's' : '' }} • {{ order.totalAmount | inrCurrency }}
              </div>
            </div>
            <div style="text-align:right">
              <app-status-badge [status]="order.status" />
              <div>
                <button class="link-btn" [id]="'order-detail-' + order.orderId" (click)="viewDetails(order.orderId)">View details →</button>
              </div>
            </div>
          </div>
        } @empty {
          <div class="panel empty-orders">
            <div style="font-size:48px;text-align:center">📦</div>
            <h3 style="text-align:center">No orders yet</h3>
            <p class="small" style="text-align:center">
              When you place your first order, it will show up here.
            </p>
            <div style="text-align:center;margin-top:16px">
              <a routerLink="/products" class="primary-btn" style="text-decoration:none">Start shopping</a>
            </div>
          </div>
        }
      }
    </div>
  `,
  styles: [`
    .order-card {
      padding: 20px;
      margin-bottom: 14px;
      display: grid;
      grid-template-columns: 1fr auto;
      gap: 14px;
      align-items: center;
    }
    .order-card strong { color: var(--text); }
    .order-meta { color: var(--muted); font-size: 14px; margin-top: 7px; }
    .empty-orders { padding: 40px 20px; }
    .loading-state {
      text-align: center;
      padding: 80px 20px;
      color: var(--muted);
    }
    .loading-spinner { font-size: 48px; margin-bottom: 12px; }
  `],
})
export class OrdersComponent implements OnInit {
  private orderService = inject(OrderService);
  private router = inject(Router);
  orders = signal<Order[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.orderService.getOrders().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  viewDetails(orderId: string): void {
    this.router.navigate(['/orders', orderId]);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  }
}
