import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Order, CreateOrderRequest } from '../models/order.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/orders`;

  create(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.baseUrl, request);
  }

  list(customerId?: string, page = 0, size = 20): Observable<PageResponse<Order>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (customerId) params = params.set('customerId', customerId);
    return this.http.get<PageResponse<Order>>(this.baseUrl, { params });
  }

  findById(id: string): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/${id}`);
  }

  cancel(id: string): Observable<Order> {
    return this.http.patch<Order>(`${this.baseUrl}/${id}/cancel`, {});
  }
}
