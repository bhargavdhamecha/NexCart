import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { InrCurrencyPipe } from '../../shared/pipes/inr-currency.pipe';
import { Product } from '../../core/models/product.model';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [RouterLink, InrCurrencyPipe],
  template: `
    <div class="container page-wrap">
      @if (product(); as p) {
        <div class="breadcrumb">
          <a routerLink="/products">Products</a> / <span>{{ p.title }}</span>
        </div>

        <div class="detail-layout">
          <!-- Product image -->
          <div class="detail-image">
            @if (p.images.length) {
              <img class="product-image" [src]="p.images[0].url" [alt]="p.title">
            } @else {
              {{ p.emoji }}
            }
          </div>

          <!-- Product info -->
          <div class="detail-info">
            <div class="stock">● In stock</div>
            <h1>{{ p.title }}</h1>
            <div class="rating">★★★★★ <span class="small">({{ reviewCount() }} reviews)</span></div>
            <div class="detail-price">{{ p.price | inrCurrency }}</div>
            <p>{{ p.description }}</p>

            <div class="panel why-nexcart">
              <strong>Why buy from NexCart?</strong>
              <div class="small" style="margin-top:8px;line-height:1.7">
                ✓ Secure checkout &nbsp; ✓ Flexible payments &nbsp; ✓ Order updates
              </div>
            </div>

            <!-- Quantity selector -->
            <div class="qty">
              <strong>Quantity</strong>
              <button (click)="changeQty(-1)" id="qty-decrease-btn">−</button>
              <span id="qty-value">{{ qty() }}</span>
              <button (click)="changeQty(1)" id="qty-increase-btn">+</button>
            </div>

            <!-- CTA buttons -->
            <div class="cta-row">
              <button class="primary-btn" (click)="addToCart()" id="add-to-cart-btn">Add to cart</button>
              <button class="secondary-btn" (click)="buyNow()" id="buy-now-btn">Buy now</button>
            </div>
          </div>
        </div>
      } @else if (loading()) {
        <div class="loading-state">
          <div class="loading-spinner">⌛</div>
          <p>Loading product...</p>
        </div>
      } @else {
        <div class="error-state">
          <h2>Product not found</h2>
          <a routerLink="/products" class="primary-btn">Browse products</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .detail-layout {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 34px;
      align-items: start;
    }
    .detail-image {
      min-height: 500px;
      display: grid;
      place-items: center;
      background: linear-gradient(145deg, #f0f4ff, #ece8ff);
      border-radius: 22px;
      font-size: 160px;
      overflow: hidden;
    }
    .detail-image .product-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    [data-theme="dark"] .detail-image {
      background: linear-gradient(145deg, #1e2240, #241b40);
    }
    .detail-info { padding: 10px 4px; }
    .stock { font-size: 12px; color: var(--accent); font-weight: 850; }
    h1 {
      font-size: 40px;
      line-height: 1.1;
      letter-spacing: -1.2px;
      margin: 10px 0;
      color: var(--text);
    }
    .rating { font-size: 14px; color: #f59e0b; margin-bottom: 4px; }
    .detail-price {
      font-size: 34px;
      font-weight: 900;
      margin: 12px 0;
      color: var(--text);
    }
    p { color: var(--muted); line-height: 1.7; }
    .why-nexcart {
      padding: 18px;
      margin: 24px 0;
    }
    .qty {
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 22px 0;
    }
    .qty button {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      border: 1px solid var(--line);
      background: var(--surface);
      color: var(--text);
      font-size: 18px;
      cursor: pointer;
      transition: .2s;
    }
    .qty button:hover { background: var(--surface-2); }
    .qty span {
      min-width: 30px;
      text-align: center;
      font-weight: 800;
      font-size: 18px;
      color: var(--text);
    }
    .cta-row { display: flex; gap: 12px; flex-wrap: wrap; }
    .loading-state, .error-state {
      text-align: center;
      padding: 80px 20px;
      color: var(--muted);
    }
    .loading-spinner { font-size: 48px; margin-bottom: 12px; }
    @media(max-width:980px) {
      .detail-layout { grid-template-columns: 1fr; }
      .detail-image { min-height: 330px; font-size: 110px; }
    }
  `],
})
export class ProductDetailComponent implements OnInit {
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private toastService = inject(ToastService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  product = signal<Product | undefined>(undefined);
  loading = signal(true);
  qty = signal(1);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.productService.getProductById(id).subscribe({
      next: (p) => {
        this.product.set(p);
        this.loading.set(false);
      },
      error: () => {
        // 404/etc — leave product() undefined so the template falls into the "not found" state.
        this.loading.set(false);
      },
    });
  }

  reviewCount(): number {
    // Cosmetic/fake review count (no backend field for it) — a deterministic string hash
    // instead of parseInt(productId), since productId is a UUID and parseInt on it is NaN.
    const id = this.product()?.productId ?? '';
    const hash = Array.from(id).reduce((sum, c) => sum + c.charCodeAt(0), 0);
    return [124, 87, 213, 56, 99, 142, 67, 38][hash % 8] ?? 100;
  }

  changeQty(delta: number): void {
    this.qty.update((q) => Math.max(1, Math.min(9, q + delta)));
  }

  addToCart(): void {
    const p = this.product();
    if (!p) return;
    this.cartService.addToCart(p.productId, this.qty());
    this.toastService.success(`${p.title} added to cart`);
  }

  buyNow(): void {
    this.addToCart();
    this.router.navigate(['/cart']);
  }
}
