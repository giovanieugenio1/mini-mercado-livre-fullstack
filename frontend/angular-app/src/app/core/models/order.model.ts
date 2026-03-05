export interface OrderItem {
  id: string;
  productId: string;
  productTitle: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: string;
  customerId: string;
  status: string;
  totalAmount: number;
  currency: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrderRequest {
  customerId: string;
  items: { productId: string; productTitle: string; unitPrice: number; quantity: number }[];
}
