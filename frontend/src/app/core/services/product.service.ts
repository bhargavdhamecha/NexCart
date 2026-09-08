import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, ProductImage, ProductListResponse } from '../models/product.model';
import { environment } from '../../../environments/environment';

export interface ProductQuery {
  categoryId?: string;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  constructor(private http: HttpClient) {}

  getProducts(query: ProductQuery = {}): Observable<ProductListResponse> {
    let params = new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 20);
    if (query.categoryId) params = params.set('categoryId', query.categoryId);
    if (query.search) params = params.set('search', query.search);
    if (query.sort) params = params.set('sort', query.sort);

    return this.http.get<ProductListResponse>(`${environment.apiUrl}/products`, { params });
  }

  getProductById(id: string): Observable<Product> {
    return this.http.get<Product>(`${environment.apiUrl}/products/${id}`);
  }

  uploadProductImage(productId: string, file: File): Observable<ProductImage> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ProductImage>(`${environment.apiUrl}/products/${productId}/images`, formData);
  }

  deleteProductImage(productId: string, imageId: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/products/${productId}/images/${imageId}`);
  }
}
