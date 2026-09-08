import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Order } from '../models/order.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class OrderService {
  /** Latest placed order (set after checkout) — lets PaymentComponent read the order id
   *  without an extra fetch. */
  latestOrder = signal<Order | null>(null);

  constructor(private http: HttpClient) {}

  getOrders(): Observable<Order[]> {
    return this.http.get<Order[]>(`${environment.apiUrl}/orders`);
  }

  getOrderById(id: string): Observable<Order> {
    return this.http.get<Order>(`${environment.apiUrl}/orders/${id}`);
  }

  checkout(): Observable<Order> {
    return this.http
      .post<Order>(`${environment.apiUrl}/orders`, {})
      .pipe(tap((order) => this.latestOrder.set(order)));
  }
}
