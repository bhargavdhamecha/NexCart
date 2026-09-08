import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { InrCurrencyPipe } from '../../shared/pipes/inr-currency.pipe';
import { OrderStepperComponent } from '../../shared/components/order-stepper/order-stepper.component';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [RouterLink, InrCurrencyPipe, OrderStepperComponent],
  template: `
    <div class="container page-wrap">
      <div class="breadcrumb">Home / Cart</div>
      <app-order-stepper currentStep="cart" />
      <div class="section-head">
        <div>
          <h2>Your cart</h2>
          <p>Review items before checkout.</p>
        </div>
      </div>

      <div class="cart-layout">
        <!-- Cart items -->
        <div class="panel cart-list" id="cart-items-panel">
          @if (!cart.loaded()) {
            <div class="loading-state">
              <div class="loading-spinner">⌛</div>
              <p>Loading your cart...</p>
            </div>
          } @else if (cart.cart().items.length === 0) {
            <div class="empty-cart">
              <div style="font-size:48px">🛒</div>
              <h3>Your cart is empty</h3>
              <p class="small">Add something you like and it will appear here.</p>
              <a routerLink="/products" class="primary-btn" style="margin-top:16px;text-decoration:none" id="empty-cart-shop-btn">Start shopping</a>
            </div>
          } @else {
            @for (item of cart.cart().items; track item.productId) {
              <div class="cart-item">
                <div class="cart-img">
                  @if (item.imageUrl) {
                    <img [src]="item.imageUrl" [alt]="item.title">
                  } @else {
                    📦
                  }
                </div>
                <div>
                  <strong>{{ item.title }}</strong>
                  @if (!item.available) {
                    <span class="unavailable-badge">No longer available</span>
                  }
                  <div class="small" style="margin-top:5px">{{ item.price | inrCurrency }}</div>
                  <div class="qty">
                    <button (click)="updateQty(item.productId, item.quantity, -1)" id="qty-dec-{{ item.productId }}">−</button>
                    <span>{{ item.quantity }}</span>
                    <button (click)="updateQty(item.productId, item.quantity, 1)" id="qty-inc-{{ item.productId }}">+</button>
                  </div>
                </div>
                <div style="text-align:right">
                  <strong>{{ item.lineTotal | inrCurrency }}</strong>
                  <div>
                    <button class="link-btn" style="color:var(--danger)" (click)="remove(item.productId)" [id]="'remove-' + item.productId">Remove</button>
                  </div>
                </div>
              </div>
            }
          }
        </div>

        <!-- Order summary -->
        <aside class="panel summary">
          <h3>Order summary</h3>
          <div class="summary-row">
            <span>Subtotal</span>
            <strong>{{ cart.cartTotal() | inrCurrency }}</strong>
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
            (click)="checkout()"
            id="proceed-checkout-btn"
            [disabled]="cart.cart().items.length === 0"
          >
            Proceed to checkout
          </button>
        </aside>
      </div>
    </div>
  `,
  styles: [`
    .cart-layout {
      display: grid;
      grid-template-columns: 1fr 340px;
      gap: 22px;
    }
    .cart-list { padding: 6px 20px; }
    .empty-cart {
      padding: 60px;
      text-align: center;
      color: var(--muted);
    }
    .empty-cart h3 { color: var(--text); }
    .loading-state {
      text-align: center;
      padding: 80px 20px;
      color: var(--muted);
    }
    .loading-spinner { font-size: 48px; margin-bottom: 12px; }
    .cart-item {
      display: grid;
      grid-template-columns: 90px 1fr auto;
      gap: 16px;
      align-items: center;
      padding: 20px 0;
      border-bottom: 1px solid var(--line);
    }
    .cart-item:last-child { border-bottom: 0; }
    .cart-img {
      height: 82px;
      border-radius: 14px;
      background: linear-gradient(145deg, #f0f4ff, #ece8ff);
      display: grid;
      place-items: center;
      font-size: 38px;
      overflow: hidden;
    }
    .cart-img img { width: 100%; height: 100%; object-fit: cover; }
    [data-theme="dark"] .cart-img { background: linear-gradient(145deg, #1e2240, #241b40); }
    strong { color: var(--text); }
    .unavailable-badge {
      display: inline-block;
      margin-left: 8px;
      font-size: 11px;
      font-weight: 700;
      color: var(--danger);
      background: rgba(239,68,68,.1);
      padding: 2px 8px;
      border-radius: 999px;
    }
    .qty {
      display: flex;
      align-items: center;
      gap: 10px;
      margin: 10px 0 0;
    }
    .qty button {
      width: 34px;
      height: 34px;
      border-radius: 8px;
      border: 1px solid var(--line);
      background: var(--surface);
      color: var(--text);
      cursor: pointer;
      transition: .2s;
    }
    .qty button:hover { background: var(--surface-2); }
    .qty span { min-width: 24px; text-align: center; font-weight: 700; color: var(--text); }
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
      .cart-layout { grid-template-columns: 1fr; }
    }
    @media(max-width:680px) {
      .cart-item { grid-template-columns: 70px 1fr; }
      .cart-item > div:last-child { grid-column: 2; }
    }
  `],
})
export class CartComponent {
  cart = inject(CartService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  updateQty(productId: string, currentQuantity: number, delta: number): void {
    const next = currentQuantity + delta;
    if (next <= 0) {
      this.remove(productId);
      return;
    }
    this.cart.setQuantity(productId, next);
  }

  remove(productId: string): void {
    this.cart.removeItem(productId);
    this.toastService.show('Item removed');
  }

  checkout(): void {
    this.router.navigate(['/checkout']);
  }
}
