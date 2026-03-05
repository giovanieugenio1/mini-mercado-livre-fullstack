import { Injectable, signal, computed } from '@angular/core';
import { CartItem } from '../models/cart.model';
import { Product } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class CartService {
  private _items = signal<CartItem[]>([]);

  readonly items = this._items.asReadonly();
  readonly count = computed(() => this._items().reduce((sum, i) => sum + i.quantity, 0));
  readonly total = computed(() => this._items().reduce((sum, i) => sum + i.product.price * i.quantity, 0));

  addItem(product: Product, quantity = 1): void {
    const current = this._items();
    const idx = current.findIndex(i => i.product.id === product.id);
    if (idx >= 0) {
      const updated = [...current];
      updated[idx] = { ...updated[idx], quantity: updated[idx].quantity + quantity };
      this._items.set(updated);
    } else {
      this._items.set([...current, { product, quantity }]);
    }
  }

  updateQuantity(productId: string, quantity: number): void {
    if (quantity <= 0) { this.removeItem(productId); return; }
    this._items.update(items => items.map(i => i.product.id === productId ? { ...i, quantity } : i));
  }

  removeItem(productId: string): void {
    this._items.update(items => items.filter(i => i.product.id !== productId));
  }

  clear(): void {
    this._items.set([]);
  }
}
