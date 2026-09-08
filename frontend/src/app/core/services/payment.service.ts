import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Payment } from '../models/payment.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  constructor(private http: HttpClient) {}

  initiate(orderId: string, idempotencyKey: string): Observable<Payment> {
    return this.http.post<Payment>(`${environment.apiUrl}/payments/initiate`, {
      orderId,
      idempotencyKey,
      paymentMethod: 'RAZORPAY',
    });
  }

  verify(paymentId: string, razorpayPaymentId: string, razorpaySignature: string): Observable<Payment> {
    return this.http.post<Payment>(`${environment.apiUrl}/payments/${paymentId}/verify`, {
      razorpayPaymentId,
      razorpaySignature,
    });
  }
}
