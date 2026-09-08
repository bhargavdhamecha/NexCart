import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <footer class="footer">
      <div class="container">
        <div class="footer-grid">
          <div>
            <div class="logo">
              <span class="logo-mark">N</span>
              NexCart
            </div>
            <p>Tech, fashion and home essentials. All in one cart.</p>
          </div>
          <div>
            <h4>Shop</h4>
            <p>
              <a routerLink="/products">Products</a><br>
              <a routerLink="/cart">Cart</a><br>
              <a routerLink="/orders">Orders</a>
            </p>
          </div>
          <div>
            <h4>Account</h4>
            <p>
              <a routerLink="/login">Sign in</a><br>
              <a routerLink="/register">Register</a>
            </p>
          </div>
          <div>
            <h4>Why NexCart?</h4>
            <p>Secure checkout<br>Flexible payments<br>Order updates</p>
          </div>
        </div>
        <div class="footer-bottom">
          <span>© 2026 NexCart</span>
          <span>Everyday essentials. One cart.</span>
        </div>
      </div>
    </footer>
  `,
  styles: [`
    .footer {
      background: #111827;
      color: #cbd5e1;
      padding: 44px 0 24px;
    }
    .footer-grid {
      display: grid;
      grid-template-columns: 2fr repeat(3, 1fr);
      gap: 30px;
    }
    .logo {
      display: flex;
      align-items: center;
      gap: 10px;
      font-weight: 850;
      font-size: 20px;
      color: white;
      margin-bottom: 12px;
    }
    .logo-mark {
      width: 34px;
      height: 34px;
      border-radius: 10px;
      background: linear-gradient(135deg, #2e5cff, #7c4dff);
      display: grid;
      place-items: center;
      color: white;
      font-size: 16px;
      font-weight: 900;
    }
    .footer h4 { color: white; margin-top: 0; }
    .footer p, .footer a {
      color: #9ca3af;
      font-size: 14px;
      line-height: 1.8;
      text-decoration: none;
    }
    .footer a:hover { color: #e2e8f0; }
    .footer-bottom {
      border-top: 1px solid #273244;
      margin-top: 30px;
      padding-top: 18px;
      font-size: 13px;
      color: #8792a6;
      display: flex;
      justify-content: space-between;
    }
    @media(max-width:680px) {
      .footer-grid { grid-template-columns: 1fr 1fr; }
      .footer-bottom { flex-direction: column; gap: 8px; }
    }
  `],
})
export class FooterComponent {}
