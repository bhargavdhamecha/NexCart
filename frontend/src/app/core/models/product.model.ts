import { Category } from './category.model';

export interface ProductImage {
  id: string;
  url: string;
  displayOrder: number;
  primary: boolean;
}

export interface Product {
  productId: string;
  title: string;
  description: string;
  price: number;
  status: 'ACTIVE' | 'INACTIVE';
  category: Category;
  availableQuantity: number;
  images: ProductImage[];
  emoji?: string; // prototype-only fallback display, used while a product has no real images
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductListResponse {
  content: Product[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
