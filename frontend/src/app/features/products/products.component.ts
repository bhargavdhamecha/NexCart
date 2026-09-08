import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { ToastService } from '../../core/services/toast.service';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { Product } from '../../core/models/product.model';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [RouterLink, FormsModule, ProductCardComponent],
  template: `
    <div class="container page-wrap">
      <div class="breadcrumb">Home / Products</div>
      <div class="section-head">
        <div>
          <h2>Explore products</h2>
          <p>Browse by category. Shop by budget.</p>
        </div>
      </div>

      <div class="products-layout">
        <!-- Filter panel -->
        <aside class="panel filter-panel">
          <h3 style="margin-top:0">Filters</h3>
          <div class="filter-group">
            <h4>Category</h4>
            <label><input type="checkbox" [(ngModel)]="filters.electronics"> Electronics</label>
            <label><input type="checkbox" [(ngModel)]="filters.fashion"> Fashion</label>
            <label><input type="checkbox" [(ngModel)]="filters.home"> Home</label>
            <label><input type="checkbox" [(ngModel)]="filters.books"> Books</label>
          </div>
          <div class="filter-group">
            <h4>Availability</h4>
            <label><input type="checkbox" [(ngModel)]="filters.inStock"> In stock</label>
          </div>
          <div class="filter-group">
            <h4>Price</h4>
            <label><input type="radio" name="price" value="under1000" [(ngModel)]="filters.priceRange"> Under ₹1,000</label>
            <label><input type="radio" name="price" value="1000to10000" [(ngModel)]="filters.priceRange"> ₹1,000–₹10,000</label>
            <label><input type="radio" name="price" value="above10000" [(ngModel)]="filters.priceRange"> ₹10,000+</label>
          </div>
        </aside>

        <!-- Products grid -->
        <div>
          <div class="toolbar">
            <div class="small">Showing <strong>{{ filteredProducts().length }} products</strong></div>
            <select [(ngModel)]="sortBy" id="sort-select" class="sort-select">
              <option value="recommended">Recommended</option>
              <option value="priceLow">Price: Low to High</option>
              <option value="priceHigh">Price: High to Low</option>
            </select>
          </div>
          <div class="grid">
            @for (product of sortedProducts(); track product.productId) {
              <app-product-card
                [product]="product"
                (addToCart)="onAddToCart($event)"
              />
            } @empty {
              <div class="empty-state">
                <div class="empty-icon">🔍</div>
                <h3>No products found</h3>
                <p class="small">Try adjusting your filters.</p>
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .products-layout {
      display: grid;
      grid-template-columns: 240px 1fr;
      gap: 22px;
    }
    .filter-panel {
      padding: 20px;
      height: max-content;
      position: sticky;
      top: 98px;
    }
    .filter-group {
      padding: 16px 0;
      border-bottom: 1px solid var(--line);
    }
    .filter-group:last-child { border-bottom: 0; }
    .filter-group h4 { margin: 0 0 12px; color: var(--text); }
    .filter-group label {
      display: flex;
      gap: 8px;
      align-items: center;
      margin: 10px 0;
      color: var(--muted);
      font-size: 14px;
      cursor: pointer;
    }
    .filter-group input[type="checkbox"],
    .filter-group input[type="radio"] {
      accent-color: var(--brand);
      width: 16px;
      height: 16px;
    }
    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 14px;
      margin-bottom: 16px;
    }
    .sort-select {
      border: 1px solid var(--line);
      border-radius: 12px;
      padding: 10px 12px;
      background: var(--surface);
      color: var(--text);
      outline: none;
    }
    .empty-state {
      grid-column: 1 / -1;
      text-align: center;
      padding: 60px 20px;
      color: var(--muted);
    }
    .empty-icon { font-size: 48px; margin-bottom: 12px; }
    @media(max-width:980px) {
      .products-layout { grid-template-columns: 1fr; }
      .filter-panel { position: static; }
    }
  `],
})
export class ProductsComponent implements OnInit {
  private productService = inject(ProductService);
  private cartService = inject(CartService);
  private toastService = inject(ToastService);
  private route = inject(ActivatedRoute);

  allProducts = signal<Product[]>([]);
  sortBy = 'recommended';
  filters = { electronics: false, fashion: false, home: false, books: false, inStock: false, priceRange: '' };

  filteredProducts = computed(() => {
    // Simple filter — in real app would filter by category tags from backend
    return this.allProducts();
  });

  sortedProducts = computed(() => {
    const products = [...this.filteredProducts()];
    if (this.sortBy === 'priceLow') return products.sort((a, b) => a.price - b.price);
    if (this.sortBy === 'priceHigh') return products.sort((a, b) => b.price - a.price);
    return products;
  });

  ngOnInit(): void {
    this.productService.getProducts().subscribe((response) => {
      this.allProducts.set(response.content);
    });
  }

  onAddToCart(product: Product): void {
    this.cartService.addToCart(product.productId);
    this.toastService.success(`${product.title} added to cart`);
  }
}
