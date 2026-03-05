import { Component, inject, signal, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { Product } from '../../../core/models/product.model';
import { getProductImageUrl } from '../../../core/utils/image.utils';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.css'
})
export class ProductDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private productService = inject(ProductService);
  private cartService = inject(CartService);

  product = signal<Product | null>(null);
  loading = signal(true);
  error = signal('');
  quantity = signal(1);
  added = signal(false);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.productService.findById(id).subscribe({
      next: p => { this.product.set(p); this.loading.set(false); },
      error: () => { this.error.set('Produto nao encontrado.'); this.loading.set(false); }
    });
  }

  increment(): void { this.quantity.update(q => q + 1); }
  decrement(): void { if (this.quantity() > 1) this.quantity.update(q => q - 1); }

  getImageUrl(product: Product): string {
    return getProductImageUrl(product.title, product.imageUrl);
  }

  onImgError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  addToCart(): void {
    const p = this.product();
    if (!p) return;
    this.cartService.addItem(p, this.quantity());
    this.added.set(true);
    setTimeout(() => this.added.set(false), 2000);
  }

  formatPrice(price: number): string {
    return price.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }
}
