export interface Product {
  id: string;
  title: string;
  description: string;
  price: number;
  stock: number;
  category: string;
  imageUrl: string | null;
  active: boolean;
  createdAt: string;
}
