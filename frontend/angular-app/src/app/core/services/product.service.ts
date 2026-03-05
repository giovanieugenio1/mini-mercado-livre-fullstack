import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product } from '../models/product.model';
import { PageResponse } from '../models/page.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/products`;

  list(query?: string, category?: string, page = 0, size = 12): Observable<PageResponse<Product>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (query) params = params.set('query', query);
    if (category) params = params.set('category', category);
    return this.http.get<PageResponse<Product>>(this.baseUrl, { params });
  }

  findById(id: string): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${id}`);
  }
}
