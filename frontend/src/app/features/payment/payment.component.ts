import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Order } from '../../core/models/order.model';
import { Payment } from '../../core/models/payment.model';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { ToastService } from '../../core/services/toast.service';
import { InrCurrencyPipe } from '../../shared/pipes/inr-currency.pipe';
import { OrderStepperComponent } from '../../shared/components/order-stepper/order-stepper.component';
import { loadRazorpayCheckout } from '../../core/utils/load-razorpay-script';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [InrCurrencyPipe, OrderStepperComponent],
  template: `
    <div class="container page-wrap">
      <div class="breadcrumb">Checkout / Payment</div>
      <app-order-stepper currentStep="payment" />

      @if (resolving()) {
        <div class="panel loading-state">
          <div class="loading-spinner">⌛</div>
          <p>Loading your order...</p>
        </div>
      } @else if (order(); as o) {
        <div class="panel payment-box">
          <div class="center">
            <p class="small">Order #{{ o.orderId }}</p>
            <div class="payment-amount">{{ o.totalAmount | inrCurrency }}</div>
            <p class="small">Pay securely via Razorpay — card, UPI, netbanking and wallets all supported.</p>
          </div>

          <button
            type="button"
            class="primary-btn full"
            id="payment-submit-btn"
            [disabled]="submitting()"
            (click)="pay()"
          >
            {{ submitting() ? 'Processing...' : 'Pay ' + (o.totalAmount | inrCurrency) }}
          </button>

          @if (error()) {
            <div class="form-error" id="payment-error">{{ error() }}</div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .payment-box {
      max-width: 480px;
      margin: 0 auto;
      padding: 28px;
    }
    .center { text-align: center; }
    h2, .payment-amount { color: var(--text); }
    .payment-amount {
      font-size: 34px;
      font-weight: 900;
      margin: 4px 0 12px;
    }
    .form-error {
      background: #fff0f0;
      color: var(--danger);
      border-radius: 10px;
      padding: 12px;
      font-size: 13px;
      margin-top: 14px;
      text-align: center;
    }
    [data-theme="dark"] .form-error { background: rgba(239,68,68,.1); }
    .full { width: 100%; margin-top: 22px; }
    .loading-state {
      text-align: center;
      padding: 80px 20px;
      color: var(--muted);
    }
    .loading-spinner { font-size: 48px; margin-bottom: 12px; }
  `],
})
export class PaymentComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private orderService = inject(OrderService);
  private paymentService = inject(PaymentService);
  private toastService = inject(ToastService);
  private cart = inject(CartService);

  order = signal<Order | null>(null);
  resolving = signal(true);
  submitting = signal(false);
  error = signal('');

  // Generated once per page load, not per click — reusing the same key means repeat "Pay"
  // clicks (e.g. after a dismissed modal) hit initiate()'s idempotency-key lookup and reopen
  // Checkout against the SAME Razorpay order, rather than minting a new one every time.
  private idempotencyKey = crypto.randomUUID();
  // Cached after the first successful initiate() call, so a second "Pay" click skips straight
  // to reopening Checkout.
  private gatewayPayment: Payment | null = null;

  ngOnInit(): void {
    const queryOrderId = this.route.snapshot.queryParamMap.get('orderId');
    const cached = this.orderService.latestOrder();

    if (cached && (!queryOrderId || cached.orderId === queryOrderId)) {
      this.resolveOrder(cached);
      return;
    }

    if (queryOrderId) {
      this.orderService.getOrderById(queryOrderId).subscribe({
        next: (order) => this.resolveOrder(order),
        error: () => {
          this.toastService.error('Order not found. Please checkout again.');
          this.router.navigate(['/cart']);
        },
      });
      return;
    }

    this.toastService.error('No order to pay for. Please checkout again.');
    this.router.navigate(['/cart']);
  }

  async pay(): Promise<void> {
    if (this.submitting()) return;
    const orderId = this.order()?.orderId;
    if (!orderId) return;

    this.submitting.set(true);
    this.error.set('');

    try {
      const payment = this.gatewayPayment ?? await this.initiatePayment(orderId);
      if (!payment) return; // initiatePayment already surfaced the error
      this.gatewayPayment = payment;

      await loadRazorpayCheckout();
      this.openCheckout(payment, orderId);
    }
    catch {
      this.submitting.set(false);
      this.error.set('Could not open the payment window. Please try again.');
    }
  }

  private initiatePayment(orderId: string): Promise<Payment | null> {
    return new Promise((resolve) => {
      this.paymentService.initiate(orderId, this.idempotencyKey).subscribe({
        next: (payment) => resolve(payment),
        error: (err) => {
          this.submitting.set(false);
          if (err?.error?.code === 'PAYMENT_GATEWAY_NOT_CONFIGURED') {
            this.error.set('Online payment isn\'t available right now — please try again later.');
          }
          else if (err?.error?.code === 'INSUFFICIENT_STOCK') {
            this.error.set('Sorry, an item in this order just sold out. Please contact support or start a new order.');
          }
          else {
            this.toastService.error('Could not start payment. Please try again.');
          }
          resolve(null);
        },
      });
    });
  }

  private openCheckout(payment: Payment, orderId: string): void {
    const user = this.authService.currentUser();

    const rzp = new (window as any).Razorpay({
      key: payment.razorpayKeyId,
      amount: Math.round(payment.amount * 100), // rupees -> paise
      currency: payment.currency,
      order_id: payment.razorpayOrderId,
      name: 'NexCart',
      description: `Order #${orderId}`,
      prefill: user ? { name: `${user.firstName} ${user.lastName}`, email: user.email } : {},
      theme: { color: '#2e5cff' },
      handler: (response: any) => this.onCheckoutSuccess(payment, orderId, response),
      modal: {
        ondismiss: () => this.submitting.set(false),
      },
    });

    rzp.on('payment.failed', () => {
      this.submitting.set(false);
      this.error.set('Payment failed or was cancelled. You can try again.');
      // No backend call here — a failed/dismissed attempt just leaves the order in
      // PENDING_PAYMENT; clicking Pay again reopens Checkout against the same Razorpay order.
    });

    rzp.open();
  }

  private onCheckoutSuccess(payment: Payment, orderId: string, response: any): void {
    this.paymentService.verify(payment.paymentId, response.razorpay_payment_id, response.razorpay_signature)
      .subscribe({
        next: () => {
          // Backend only clears the cart once payment is actually verified — refetch so the
          // navbar badge and /cart reflect that real state rather than guessing locally.
          this.cart.loadCart();
          this.toastService.success('Payment successful');
          this.router.navigate(['/order-success', orderId]);
        },
        error: () => {
          this.submitting.set(false);
          this.error.set('We could not verify your payment. If money was deducted, please contact support.');
        },
      });
  }

  private resolveOrder(order: Order): void {
    if (order.status === 'CONFIRMED' || order.status === 'PAID') {
      this.toastService.show('This order is already paid');
      this.router.navigate(['/order-success', order.orderId]);
      return;
    }
    this.order.set(order);
    this.resolving.set(false);
  }
}
