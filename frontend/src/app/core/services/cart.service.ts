import { Injectable, signal, computed, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Cart } from '../models/cart.model';
import { AuthService } from './auth.service';
import { ToastService } from './toast.service';
import { environment } from '../../../environments/environment';

const EMPTY_CART: Cart = { cartId: '', items: [], totalAmount: 0, itemCount: 0 };

@Injectable({ providedIn: 'root' })
export class CartService {
  /** Reactive cart — server is the source of truth, this just mirrors the last response */
  cart = signal<Cart>(EMPTY_CART);
  /** Whether the first cart fetch (or the logged-out "nothing to load" case) has resolved. */
  loaded = signal(false);

  cartCount = computed(() => this.cart().itemCount);
  cartTotal = computed(() => this.cart().totalAmount);

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private router: Router,
    private toastService: ToastService
  ) {
    // Loads the cart when a user is present, resets to empty otherwise — keeps the cart in
    // sync with login/logout without CartService and AuthService depending on each other.
    effect(() => {
      if (this.authService.currentUser()) {
        this.loadCart();
      }
      else {
        this.cart.set(EMPTY_CART);
        this.loaded.set(true);
      }
    });
  }

  loadCart(): void {
    this.loaded.set(false);
    this.http.get<Cart>(`${environment.apiUrl}/cart`).subscribe((cart) => {
      this.cart.set(cart);
      this.loaded.set(true);
    });
  }

  addToCart(productId: string, quantity = 1): void {
    if (!this.requireAuth()) return;
    this.http
      .post<Cart>(`${environment.apiUrl}/cart/items`, { productId, quantity })
      .subscribe((cart) => this.cart.set(cart));
  }

  setQuantity(productId: string, quantity: number): void {
    if (!this.requireAuth()) return;
    this.http
      .put<Cart>(`${environment.apiUrl}/cart/items/${productId}`, { quantity })
      .subscribe((cart) => this.cart.set(cart));
  }

  removeItem(productId: string): void {
    if (!this.requireAuth()) return;
    this.http
      .delete<Cart>(`${environment.apiUrl}/cart/items/${productId}`)
      .subscribe((cart) => this.cart.set(cart));
  }

  clearCart(): void {
    if (!this.requireAuth()) return;
    this.http.delete<Cart>(`${environment.apiUrl}/cart`).subscribe((cart) => this.cart.set(cart));
  }

  private requireAuth(): boolean {
    if (this.authService.isAuthenticated()) return true;
    this.toastService.error('Please sign in to add items to your cart');
    this.router.navigate(['/login']);
    return false;
  }
}
