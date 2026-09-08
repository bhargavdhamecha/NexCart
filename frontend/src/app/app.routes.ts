import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/home/home.component').then((m) => m.HomeComponent),
    title: 'NexCart — E-commerce',
  },
  {
    path: 'products',
    loadComponent: () =>
      import('./features/products/products.component').then((m) => m.ProductsComponent),
    title: 'Products — NexCart',
  },
  {
    path: 'products/:id',
    loadComponent: () =>
      import('./features/product-detail/product-detail.component').then(
        (m) => m.ProductDetailComponent
      ),
    title: 'Product — NexCart',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
    canActivate: [guestGuard],
    title: 'Sign In — NexCart',
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
    canActivate: [guestGuard],
    title: 'Register — NexCart',
  },
  {
    path: 'cart',
    loadComponent: () =>
      import('./features/cart/cart.component').then((m) => m.CartComponent),
    canActivate: [authGuard],
    title: 'Your Cart — NexCart',
  },
  {
    path: 'checkout',
    loadComponent: () =>
      import('./features/checkout/checkout.component').then((m) => m.CheckoutComponent),
    canActivate: [authGuard],
    title: 'Checkout — NexCart',
  },
  {
    path: 'payment',
    loadComponent: () =>
      import('./features/payment/payment.component').then((m) => m.PaymentComponent),
    canActivate: [authGuard],
    title: 'Payment — NexCart',
  },
  {
    path: 'order-success/:id',
    loadComponent: () =>
      import('./features/order-success/order-success.component').then(
        (m) => m.OrderSuccessComponent
      ),
    canActivate: [authGuard],
    title: 'Order Confirmed — NexCart',
  },
  {
    path: 'orders',
    loadComponent: () =>
      import('./features/orders/orders.component').then((m) => m.OrdersComponent),
    canActivate: [authGuard],
    title: 'My Orders — NexCart',
  },
  {
    path: 'orders/:id',
    loadComponent: () =>
      import('./features/order-detail/order-detail.component').then((m) => m.OrderDetailComponent),
    canActivate: [authGuard],
    title: 'Order Details — NexCart',
  },
  {
    path: '**',
    redirectTo: '',
  },
];
