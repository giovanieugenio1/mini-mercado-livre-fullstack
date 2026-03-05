import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.css'
})
export class OrderDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private orderService = inject(OrderService);

  order = signal<Order | null>(null);
  loading = signal(true);
  error = signal('');
  cancelling = signal(false);
  cancelError = signal('');

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.orderService.findById(id).subscribe({
      next: o => { this.order.set(o); this.loading.set(false); },
      error: () => { this.error.set('Pedido nao encontrado.'); this.loading.set(false); }
    });
  }

  cancelOrder(): void {
    const o = this.order();
    if (!o) return;
    this.cancelling.set(true);
    this.cancelError.set('');
    this.orderService.cancel(o.id).subscribe({
      next: updated => { this.order.set(updated); this.cancelling.set(false); },
      error: (err: { error?: { detail?: string } }) => {
        this.cancelError.set(err?.error?.detail ?? 'Nao foi possivel cancelar o pedido.');
        this.cancelling.set(false);
      }
    });
  }

  canCancel(status: string): boolean { return status === 'PENDING'; }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'Pendente', CONFIRMED: 'Confirmado', PROCESSING: 'Processando',
      SHIPPED: 'Enviado', DELIVERED: 'Entregue', CANCELLED: 'Cancelado'
    };
    return map[status] ?? status;
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'badge-pending', CONFIRMED: 'badge-confirmed', PROCESSING: 'badge-processing',
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
