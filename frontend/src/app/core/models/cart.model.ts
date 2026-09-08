export interface CartItem {
  cartItemId: string;
  productId: string;
  title: string;
  price: number;
  imageUrl?: string;
  quantity: number;
  lineTotal: number;
  available: boolean;
}

export interface Cart {
  cartId: string;
  items: CartItem[];
  totalAmount: number;
  itemCount: number;
}
