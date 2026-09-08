import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { ToastService } from '../../core/services/toast.service';
import { InrCurrencyPipe } from '../../shared/pipes/inr-currency.pipe';
import { OrderStepperComponent } from '../../shared/components/order-stepper/order-stepper.component';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [RouterLink, InrCurrencyPipe, OrderStepperComponent],
  template: `
    <div class="container page-wrap">
      <div class="breadcrumb">Cart / Checkout</div>
      <app-order-stepper currentStep="review" />
      <div class="section-head">
        <div>
          <h2>Review your order</h2>
          <p>Check your items and total before payment.</p>
        </div>
      </div>

      @if (cart.cart().items.length === 0) {
        <div class="panel empty-cart">
          <div style="font-size:48px">🛒</div>
          <h3>Your cart is empty</h3>
          <p class="small">Add something you like before checking out.</p>
          <a routerLink="/cart" class="primary-btn" style="margin-top:16px;text-decoration:none">Back to cart</a>
        </div>
      } @else {
        <div class="checkout-layout">
          <!-- Order review -->
          <div class="panel review-list">
            @for (item of cart.cart().items; track item.productId) {
              <div class="review-item">
                <div class="review-img">
                  @if (item.imageUrl) {
                    <img [src]="item.imageUrl" [alt]="item.title">
                  } @else {
                    📦
                  }
                </div>
                <div>
                  <strong>{{ item.title }}</strong>
                  <div class="small" style="margin-top:5px">Qty {{ item.quantity }} × {{ item.price | inrCurrency }}</div>
                </div>
                <strong>{{ item.lineTotal | inrCurrency }}</strong>
              </div>
            }
          </div>

          <!-- Summary -->
          <aside class="panel summary">
            <h3>Order summary</h3>
            <div class="summary-row">
              <span>Items</span>
              <strong>{{ cart.cartCount() }}</strong>
            </div>
            <div class="summary-row">
              <span>Delivery</span>
              <strong>FREE</strong>
            </div>
            <div class="summary-total">
              <span>Total</span>
              <span>{{ cart.cartTotal() | inrCurrency }}</span>
            </div>
            <button
              class="primary-btn full"
              style="margin-top:20px"
              (click)="placeOrder()"
              id="place-order-btn"
              [disabled]="loading()"
            >
              {{ loading() ? 'Placing order...' : 'Place order' }}
            </button>
          </aside>
        </div>
      }
    </div>
  `,
  styles: [`
    .checkout-layout {
      display: grid;
      grid-template-columns: 1fr 340px;
      gap: 22px;
    }
    .empty-cart {
      padding: 60px;
      text-align: center;
      color: var(--muted);
    }
    .empty-cart h3 { color: var(--text); }
    .review-list { padding: 6px 20px; }
    .review-item {
      display: grid;
      grid-template-columns: 70px 1fr auto;
      gap: 16px;
      align-items: center;
      padding: 18px 0;
      border-bottom: 1px solid var(--line);
    }
    .review-item:last-child { border-bottom: 0; }
    .review-img {
      height: 70px;
      border-radius: 14px;
      background: linear-gradient(145deg, #f0f4ff, #ece8ff);
      display: grid;
      place-items: center;
      font-size: 32px;
      overflow: hidden;
    }
    .review-img img { width: 100%; height: 100%; object-fit: cover; }
    [data-theme="dark"] .review-img { background: linear-gradient(145deg, #1e2240, #241b40); }
    strong { color: var(--text); }
    .summary { padding: 22px; height: max-content; }
    .summary h3 { margin-top: 0; color: var(--text); }
    .summary-row {
      display: flex;
      justify-content: space-between;
      margin: 13px 0;
      color: var(--muted);
    }
    .summary-row strong { color: var(--text); }
    .summary-total {
      display: flex;
      justify-content: space-between;
      font-size: 20px;
      font-weight: 900;
      border-top: 1px solid var(--line);
      padding-top: 16px;
      margin-top: 14px;
      color: var(--text);
    }
    @media(max-width:980px) {
      .checkout-layout { grid-template-columns: 1fr; }
    }
    @media(max-width:680px) {
      .review-item { grid-template-columns: 56px 1fr; }
      .review-item > strong:last-child { grid-column: 2; justify-self: start; }
    }
  `],
})
export class CheckoutComponent {
  cart = inject(CartService);
  private orderService = inject(OrderService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  loading = signal(false);

  placeOrder(): void {
    this.loading.set(true);
    this.orderService.checkout().subscribe({
      next: (order) => {
        this.loading.set(false);
        // Cart is deliberately left untouched by checkout — it's only cleared once payment
        // actually succeeds (see PaymentComponent), so a declined/abandoned payment doesn't
        // leave the user with an empty cart for items they never actually bought.
        this.router.navigate(['/payment'], { queryParams: { orderId: order.orderId } });
      },
      error: () => {
        this.loading.set(false);
        this.toastService.error('Failed to place order. Please try again.');
      },
    });
  }
}
