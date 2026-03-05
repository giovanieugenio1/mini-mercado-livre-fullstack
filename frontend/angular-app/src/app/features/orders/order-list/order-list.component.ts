import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { AuthService } from '../../../core/services/auth.service';
import { Order } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './order-list.component.html',
  styleUrl: './order-list.component.css'
})
export class OrderListComponent implements OnInit {
  private orderService = inject(OrderService);
  private auth = inject(AuthService);

  orders = signal<Order[]>([]);
  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    this.orderService.list(this.auth.userId()).subscribe({
      next: res => { this.orders.set(res.content); this.loading.set(false); },
      error: () => { this.error.set('Erro ao carregar pedidos.'); this.loading.set(false); }
    });
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'Pendente', CONFIRMED: 'Confirmado', PROCESSING: 'Processando',
      SHIPPED: 'Enviado', DELIVERED: 'Entregue', CANCELLED: 'Cancelado'
    };
    return map[status] ?? status;
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'badge-pending', CONFIRMED: 'badge-confirmado', PROCESSING: 'badge-processing',
      SHIPPED: 'badge-shipped', DELIVERED: 'badge-delivered', CANCELLED: 'badge-cancelled'
    };
    return `badge ${map[status] ?? ''}`;
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  formatPrice(price: number): string {
    return price.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }
}
