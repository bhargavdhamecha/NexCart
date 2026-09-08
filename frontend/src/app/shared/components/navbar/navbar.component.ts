import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';
import { ThemeToggleComponent } from '../theme-toggle/theme-toggle.component';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, CommonModule, ThemeToggleComponent, FormsModule],
  template: `
    <!-- Top announcement bar -->
    <div class="topbar">
      <div class="container">
        <span>Everyday essentials. One cart.</span>
        <span>Secure checkout • Flexible payments • Order updates</span>
      </div>
    </div>

    <!-- Sticky main header -->
    <header class="header">
      <div class="container header-row">
        <!-- Logo -->
        <a class="logo" routerLink="/" aria-label="NexCart home">
          <span class="logo-mark">N</span>
          NexCart
        </a>

        <!-- Search bar -->
        <div class="search">
          <span style="font-size: 29px;">⌕</span>
          <input
            id="globalSearch"
            [(ngModel)]="searchQuery"
            placeholder="Search products, categories and brands"
            (keydown.enter)="onSearch()"
            aria-label="Search products"
          >
        </div>

        <!-- Nav actions -->
        <div class="nav-actions">
          <a routerLink="/orders" class="ghost-btn" id="nav-orders-btn">Orders</a>

          @if (auth.isAuthenticated()) {
            <div class="user-menu">
              <button class="ghost-btn" (click)="toggleUserMenu()" id="nav-user-btn">
                {{ auth.currentUser()?.firstName }} ▾
              </button>
              @if (userMenuOpen) {
                <div class="user-dropdown">
                  <div class="user-dropdown-name">
                    {{ auth.currentUser()?.firstName }} {{ auth.currentUser()?.lastName }}
                  </div>
                  <div class="user-dropdown-email">{{ auth.currentUser()?.email }}</div>
                  <hr class="user-dropdown-divider">
                  <button (click)="logout()" id="nav-logout-btn">Sign out</button>
                </div>
              }
            </div>
          } @else {
            <a routerLink="/login" class="ghost-btn" id="nav-signin-btn">Sign in</a>
          }

          <!-- Cart icon with badge -->
          <a routerLink="/cart" class="icon-btn" aria-label="Cart" id="nav-cart-btn">
            🛒
            @if (cart.cartCount() > 0) {
              <span class="cart-count">{{ cart.cartCount() }}</span>
            }
          </a>

          <!-- Theme toggle -->
          <app-theme-toggle />
        </div>
      </div>
    </header>
  `,
  styles: [`
    .topbar {
      background: #111827;
      color: #dbe2ef;
      font-size: 13px;
      padding: 9px 0;
    }
    .topbar .container {
      display: flex;
      justify-content: space-between;
      gap: 16px;
      align-items: center;
    }
    .header {
      position: sticky;
      top: 0;
      z-index: 40;
      background: var(--header-bg);
      backdrop-filter: blur(14px);
      border-bottom: 1px solid var(--header-border);
    }
    .header-row {
      height: 76px;
      display: flex;
      align-items: center;
      gap: 22px;
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 10px;
      font-weight: 850;
      font-size: 22px;
      letter-spacing: -.4px;
      color: var(--text);
      text-decoration: none;
      white-space: nowrap;
    }
    .logo-mark {
      width: 38px;
      height: 38px;
      border-radius: 12px;
      background: linear-gradient(135deg, var(--brand), var(--brand-2));
      display: grid;
      place-items: center;
      color: white;
      box-shadow: 0 8px 22px rgba(46,92,255,.28);
      flex-shrink: 0;
    }
    .search {
      flex: 1;
      display: flex;
      align-items: center;
      background: var(--search-bg);
      border: 1px solid var(--line);
      border-radius: 14px;
      padding: 0 14px;
      height: 46px;
      min-width: 180px;
      transition: .2s;
    }
    .search:focus-within {
      border-color: var(--brand);
      box-shadow: 0 0 0 3px rgba(46,92,255,.12);
    }
    .search input {
      border: 0;
      outline: 0;
      background: transparent;
      flex: 1;
      padding: 0 10px;
      color: var(--text);
    }
    .nav-actions {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .user-menu {
      position: relative;
    }
    .user-dropdown {
      position: absolute;
      top: calc(100% + 8px);
      right: 0;
      background: var(--surface);
      border: 1px solid var(--line);
      border-radius: 14px;
      box-shadow: var(--shadow);
      padding: 12px;
      min-width: 200px;
      z-index: 100;
    }
    .user-dropdown-name {
      font-weight: 700;
      font-size: 14px;
      color: var(--text);
    }
    .user-dropdown-email {
      font-size: 12px;
      color: var(--muted);
      margin-top: 2px;
    }
    .user-dropdown-divider {
      border: none;
      border-top: 1px solid var(--line);
      margin: 10px 0;
    }
    .user-dropdown button {
      width: 100%;
      text-align: left;
      border: 0;
      background: transparent;
      color: var(--danger);
      font-weight: 700;
      padding: 6px 4px;
      cursor: pointer;
      font-size: 14px;
      border-radius: 8px;
    }
    .user-dropdown button:hover {
      background: var(--danger-hover-bg);
    }
    @media(max-width:680px) {
      .topbar { display: none; }
      .header-row { height: auto; padding: 12px 0; flex-wrap: wrap; }
      .search { order: 3; width: 100%; flex-basis: 100%; }
      .nav-actions .ghost-btn:not(.user-menu .ghost-btn) { display: none; }
    }
  `],
})
export class NavbarComponent {
  auth = inject(AuthService);
  cart = inject(CartService);
  private router = inject(Router);

  searchQuery = '';
  userMenuOpen = false;

  onSearch(): void {
    if (this.searchQuery.trim()) {
      this.router.navigate(['/products'], { queryParams: { q: this.searchQuery.trim() } });
    }
  }

  toggleUserMenu(): void {
    this.userMenuOpen = !this.userMenuOpen;
  }

  logout(): void {
    this.userMenuOpen = false;
    this.auth.logout();
  }
}
