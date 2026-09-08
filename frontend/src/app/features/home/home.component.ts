import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { Product } from '../../core/models/product.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, ProductCardComponent],
  template: `
    <!-- Hero Banner -->
    <div class="container">
      <div class="hero">
        <div class="hero-copy">
          <span class="badge">✨ Discover. Shop. Enjoy.</span>
          <h1>Everything you want,<br>one cart away.</h1>
          <p>Tech, fashion and home essentials. Simple shopping, start to finish.</p>
          <div class="hero-cta">
            <a routerLink="/products" class="primary-btn" id="hero-shop-btn">Shop now →</a>
            <a routerLink="/orders" class="ghost-btn" id="hero-orders-btn">View my orders</a>
          </div>
        </div>
        <div class="hero-art">
          <div class="device-card">
            <div class="floating-chip chip-1">✓ Secure checkout</div>
            <div class="floating-chip chip-2">📦 Order updates</div>
          </div>
        </div>
      </div>

      <!-- Categories -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2>Shop by category</h2>
            <p>Find what you need faster.</p>
          </div>
        </div>
        <div class="categories">
          <a routerLink="/products" class="category" id="cat-electronics">
            <div class="category-icon">💻</div><strong>Electronics</strong>
          </a>
          <a routerLink="/products" class="category" id="cat-fashion">
            <div class="category-icon">👟</div><strong>Fashion</strong>
          </a>
          <a routerLink="/products" class="category" id="cat-home">
            <div class="category-icon">🏠</div><strong>Home</strong>
          </a>
          <a routerLink="/products" class="category" id="cat-accessories">
            <div class="category-icon">🎧</div><strong>Accessories</strong>
          </a>
          <a routerLink="/products" class="category" id="cat-books">
            <div class="category-icon">📚</div><strong>Books</strong>
          </a>
        </div>
      </section>

      <!-- Popular products -->
      <section class="section">
        <div class="section-head">
          <div>
            <h2>Explore the collection</h2>
            <p>Your next find awaits.</p>
          </div>
          <a routerLink="/products" class="link-btn" id="home-view-all-btn">View all →</a>
        </div>
        <div class="grid">
          @for (product of featuredProducts(); track product.productId) {
            <app-product-card
              [product]="product"
              (addToCart)="onAddToCart($event)"
              (wishlist)="onWishlist($event)"
            />
          }
        </div>
      </section>

      <!-- Trust section -->
      <section class="section">
        <div class="trust">
          <div class="trust-card">
            <div class="trust-icon">🔒</div>
            <div>
              <strong>Flexible payments</strong>
              <div class="small">UPI, cards, netbanking and wallets.</div>
            </div>
          </div>
          <div class="trust-card">
            <div class="trust-icon">🔎</div>
            <div>
              <strong>Easy discovery</strong>
              <div class="small">Browse by category. Filter by budget.</div>
            </div>
          </div>
          <div class="trust-card">
            <div class="trust-icon">📦</div>
            <div>
              <strong>Order updates</strong>
              <div class="small">View order status and past purchases.</div>
            </div>
          </div>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .hero {
      margin: 28px auto 22px;
      border-radius: 28px;
      overflow: hidden;
      background:
        radial-gradient(circle at 82% 24%, rgba(255,255,255,.55), transparent 18%),
        radial-gradient(circle at 70% 75%, rgba(255,255,255,.2), transparent 20%),
        linear-gradient(135deg, #1f48ff 0%, #6247e7 54%, #9d63ff 100%);
      color: white;
      min-height: 370px;
      display: grid;
      grid-template-columns: 1.15fr .85fr;
      align-items: center;
      box-shadow: 0 18px 50px rgba(49,65,180,.25);
    }
    .hero-copy { padding: 56px; }
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      border-radius: 999px;
      padding: 8px 12px;
      background: rgba(255,255,255,.14);
      border: 1px solid rgba(255,255,255,.24);
      font-size: 13px;
      font-weight: 750;
    }
    .hero h1 {
      font-size: clamp(38px, 5vw, 62px);
      line-height: 1.0;
      margin: 20px 0 18px;
      letter-spacing: -2px;
    }
    .hero p {
      font-size: 18px;
      line-height: 1.6;
      color: #edf0ff;
      max-width: 640px;
    }
    .hero-cta { display: flex; gap: 12px; margin-top: 26px; flex-wrap: wrap; }
    .hero .primary-btn {
      background: white;
      color: #2f45bd;
      box-shadow: none;
      text-decoration: none;
    }
    .hero .ghost-btn {
      background: rgba(255,255,255,.12);
      color: white;
      border-color: rgba(255,255,255,.28);
      text-decoration: none;
    }
    .hero-art { padding: 30px 36px 30px 0; }
    .device-card {
      width: 100%;
      aspect-ratio: 1/1;
      max-width: 340px;
      margin-left: auto;
      border-radius: 28px;
      background: linear-gradient(145deg, #ffffff, #e9eeff);
      position: relative;
      box-shadow: 0 30px 55px rgba(22,28,70,.28);
      overflow: hidden;
    }
    .device-card::before {
      content: '';
      position: absolute;
      width: 190px;
      height: 190px;
      border-radius: 50%;
      background: linear-gradient(135deg, #ffca6a, #ff8d6c);
      left: 46px;
      top: 48px;
      filter: drop-shadow(0 20px 20px rgba(245,138,90,.25));
    }
    .device-card::after {
      content: 'NC';
      position: absolute;
      right: 30px;
      bottom: 22px;
      color: #233465;
      font-size: 64px;
      font-weight: 900;
      letter-spacing: -5px;
      opacity: .92;
    }
    .floating-chip {
      position: absolute;
      background: white;
      color: #27324e;
      border-radius: 14px;
      padding: 11px 13px;
      font-weight: 800;
      box-shadow: 0 12px 40px rgba(25,35,60,.09);
      font-size: 13px;
    }
    .chip-1 { left: 18px; bottom: 30px; }
    .chip-2 { right: 18px; top: 20px; }
    .categories {
      display: grid;
      grid-template-columns: repeat(5,1fr);
      gap: 14px;
    }
    .category {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 16px;
      padding: 18px;
      display: flex;
      align-items: center;
      gap: 12px;
      box-shadow: 0 5px 18px rgba(20,30,55,.04);
      transition: .2s;
      text-decoration: none;
      color: var(--text);
    }
    .category:hover { transform: translateY(-2px); box-shadow: var(--shadow); }
    .category-icon {
      width: 42px;
      height: 42px;
      border-radius: 12px;
      background: linear-gradient(135deg, #edf3ff, #eee8ff);
      display: grid;
      place-items: center;
      font-size: 20px;
    }
    [data-theme="dark"] .category-icon {
      background: linear-gradient(135deg, #1e2540, #231b40);
    }
    .category strong { font-size: 14px; }
    .trust {
      display: grid;
      grid-template-columns: repeat(3,1fr);
      gap: 16px;
      margin-top: 10px;
    }
    .trust-card {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 18px;
      padding: 20px;
      display: flex;
      gap: 14px;
      align-items: flex-start;
    }
    .trust-icon {
      width: 42px;
      height: 42px;
      flex: 0 0 42px;
      border-radius: 12px;
      background: #eef3ff;
      display: grid;
      place-items: center;
      font-size: 20px;
    }
    [data-theme="dark"] .trust-icon { background: #1e2540; }
    @media(max-width:980px) {
      .categories { grid-template-columns: repeat(3,1fr); }
      .hero { grid-template-columns: 1fr; }
      .hero-art { display: none; }
      .trust { grid-template-columns: 1fr; }
    }
    @media(max-width:680px) {
      .hero-copy { padding: 34px 24px; }
      .hero { min-height: 330px; }
      .categories { grid-template-columns: repeat(2,1fr); }
    }
  `],
})
export class HomeComponent implements OnInit {
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private toastService = inject(ToastService);

  featuredProducts = signal<Product[]>([]);

  ngOnInit(): void {
    this.productService.getProducts({ size: 4 }).subscribe((response) => {
      this.featuredProducts.set(response.content);
    });
  }

  onAddToCart(product: Product): void {
    this.cartService.addToCart(product.productId);
    this.toastService.success(`${product.title} added to cart`);
  }

  onWishlist(product: Product): void {
    this.toastService.show(`Saved to wishlist`);
  }
}
