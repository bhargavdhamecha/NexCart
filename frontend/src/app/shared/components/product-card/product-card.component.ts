import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Product } from '../../../core/models/product.model';
import { InrCurrencyPipe } from '../../pipes/inr-currency.pipe';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [InrCurrencyPipe, RouterLink],
  template: `
    <article class="product-card">
      <div
        class="product-visual"
        [routerLink]="['/products', product.productId]"
        style="cursor:pointer"
      >
        <button
          class="heart"
          (click)="$event.stopPropagation(); onWishlist()"
          [class.hearted]="wishlisted"
          aria-label="Add to wishlist"
        >{{ wishlisted ? '❤️' : '♡' }}</button>
        @if (product.images.length) {
          <img class="product-image" [src]="product.images[0].url" [alt]="product.title">
        } @else {
          <span class="emoji">{{ product.emoji }}</span>
        }
      </div>
      <div class="stock">● In stock</div>
      <h3>{{ product.title }}</h3>
      <div class="rating">★★★★★ <span class="small">(4.{{ getRating() }})</span></div>
      <div class="price-row">
        <div class="price">{{ product.price | inrCurrency }}</div>
        <button class="add-btn" (click)="onAddToCart()" aria-label="Add to cart">+</button>
      </div>
    </article>
  `,
  styles: [`
    .product-card {
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 18px;
      padding: 14px;
      box-shadow: 0 5px 18px rgba(20,30,55,.04);
      transition: .2s;
      position: relative;
    }
    .product-card:hover {
      transform: translateY(-3px);
      box-shadow: var(--shadow);
    }
    .product-visual {
      height: 210px;
      border-radius: 14px;
      background: linear-gradient(145deg, #f0f4ff, #ece8ff);
      display: grid;
      place-items: center;
      overflow: hidden;
      position: relative;
    }
    [data-theme="dark"] .product-visual {
      background: linear-gradient(145deg, #1e2240, #241b40);
    }
    .emoji {
      font-size: 74px;
      filter: drop-shadow(0 14px 14px rgba(20,30,55,.12));
    }
    .product-image {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .heart {
      position: absolute;
      right: 12px;
      top: 12px;
      width: 34px;
      height: 34px;
      border-radius: 999px;
      border: 0;
      background: rgba(255,255,255,.85);
      font-size: 17px;
      cursor: pointer;
      transition: transform .2s;
    }
    .heart:hover, .heart.hearted { transform: scale(1.2); }
    .stock {
      font-size: 12px;
      color: var(--accent);
      font-weight: 850;
      margin-top: 13px;
    }
    h3 {
      font-size: 16px;
      margin: 6px 0;
      line-height: 1.35;
      color: var(--text);
    }
    .rating {
      font-size: 13px;
      color: #f59e0b;
    }
    .price-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: 12px;
    }
    .price {
      font-size: 20px;
      font-weight: 900;
      color: var(--text);
    }
    .add-btn {
      width: 40px;
      height: 40px;
      border: 0;
      border-radius: 12px;
      background: #111827;
      color: white;
      font-size: 21px;
      cursor: pointer;
      transition: .2s;
    }
    [data-theme="dark"] .add-btn {
      background: var(--brand);
    }
    .add-btn:hover {
      transform: scale(1.08);
      background: var(--brand);
    }
  `],
})
export class ProductCardComponent {
  @Input({ required: true }) product!: Product;
  @Output() addToCart = new EventEmitter<Product>();
  @Output() wishlist = new EventEmitter<Product>();

  wishlisted = false;

  getRating(): number {
    // Cosmetic/fake rating (no backend rating field exists) — a deterministic string hash
    // instead of parseInt(productId), since productId is a UUID and parseInt on it is NaN.
    const hash = Array.from(this.product.productId).reduce((sum, c) => sum + c.charCodeAt(0), 0);
    return (hash % 4) + 5;
  }

  onAddToCart(): void {
    this.addToCart.emit(this.product);
  }

  onWishlist(): void {
    this.wishlisted = !this.wishlisted;
    this.wishlist.emit(this.product);
  }
}
