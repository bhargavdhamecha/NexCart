export type OrderStatus = 'CREATED' | 'PENDING_PAYMENT' | 'PAID' | 'CONFIRMED' | 'PAYMENT_FAILED' | 'CANCELLED';

export interface OrderItem {
  orderItemId: string;
  productId: string;
  title: string;
  quantity: number;
  unitPrice: number;
  itemTotal: number;
  imageUrl?: string;
}

export interface Order {
  orderId: string;
  totalAmount: number;
  orderDate: string;
  status: OrderStatus;
  items: OrderItem[];
}
