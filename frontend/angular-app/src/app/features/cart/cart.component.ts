import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.css'
})
export class CartComponent {
  cart = inject(CartService);
  private orderService = inject(OrderService);
  private auth = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  error = signal('');

  get isLoggedIn(): boolean { return this.auth.isLoggedIn(); }

  updateQty(productId: string, delta: number): void {
    const item = this.cart.items().find(i => i.product.id === productId);
    if (item) this.cart.updateQuantity(productId, item.quantity + delta);
  }

  remove(productId: string): void { this.cart.removeItem(productId); }

  checkout(): void {
    if (!this.auth.isLoggedIn()) { this.router.navigate(['/login']); return; }
    if (this.cart.items().length === 0) return;

    this.loading.set(true);
    this.error.set('');

    const request = {
      customerId: this.auth.userId(),
      items: this.cart.items().map(i => ({
        productId: i.product.id,
        productTitle: i.product.title,
        unitPrice: i.product.price,
        quantity: i.quantity
      }))
    };

    this.orderService.create(request).subscribe({
      next: order => {
        this.cart.clear();
        this.loading.set(false);
        this.router.navigate(['/orders', order.id]);
      },
      error: (err: { error?: { detail?: string } }) => {
        this.error.set(err?.error?.detail ?? 'Erro ao criar pedido. Tente novamente.');
        this.loading.set(false);
      }
    });
  }

  formatPrice(price: number): string {
    return price.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }
}
