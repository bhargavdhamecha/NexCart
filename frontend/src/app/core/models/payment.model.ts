export type PaymentStatus = 'INITIATED' | 'SUCCESS' | 'FAILED' | 'REFUNDED';

export interface Payment {
  paymentId: string;
  orderId: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  transactionId?: string;
  paymentMethod: string;
  /** Razorpay's order id for this payment — passed to Checkout, not our own orderId. */
  razorpayOrderId?: string;
  /** Publishable key id — safe to use client-side as Checkout's `key` option. */
  razorpayKeyId?: string;
}
