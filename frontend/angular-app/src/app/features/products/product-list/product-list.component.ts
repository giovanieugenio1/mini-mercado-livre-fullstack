import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { Product } from '../../../core/models/product.model';
import { getProductImageUrl } from '../../../core/utils/image.utils';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.css'
})
export class ProductListComponent implements OnInit {
  private productService = inject(ProductService);
  private cartService = inject(CartService);

  products = signal<Product[]>([]);
  totalPages = signal(0);
  currentPage = signal(0);
  totalElements = signal(0);
  loading = signal(false);
  error = signal('');
  addedIds = signal<Set<string>>(new Set());

  searchQuery = '';
  selectedCategory = '';

  readonly categories = ['', 'ELETRONICOS', 'VESTUARIO', 'ESPORTES', 'CASA', 'LIVROS', 'ALIMENTOS'];
  readonly categoryLabels: Record<string, string> = {
    '': 'Todas',
    'ELETRONICOS': 'Eletrônicos',
    'VESTUARIO': 'Vestuário',
    'ESPORTES': 'Esportes',
    'CASA': 'Casa',
    'LIVROS': 'Livros',
    'ALIMENTOS': 'Alimentos',
  };

  ngOnInit(): void { this.load(); }

  load(page = 0): void {
    this.loading.set(true);
    this.error.set('');
    this.productService.list(this.searchQuery || undefined, this.selectedCategory || undefined, page)
      .subscribe({
        next: res => {
          this.products.set(res.content);
          this.totalPages.set(res.totalPages);
          this.currentPage.set(res.page);
          this.totalElements.set(res.totalElements);
          this.loading.set(false);
        },
        error: () => { this.error.set('Erro ao carregar produtos.'); this.loading.set(false); }
      });
  }

  onSearch(): void { this.load(0); }
  prevPage(): void { if (this.currentPage() > 0) this.load(this.currentPage() - 1); }
  nextPage(): void { if (this.currentPage() < this.totalPages() - 1) this.load(this.currentPage() + 1); }

  getImageUrl(product: Product): string {
    return getProductImageUrl(product.title, product.imageUrl);
  }

  onImgError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  addToCart(product: Product): void {
    this.cartService.addItem(product);
    const ids = new Set(this.addedIds());
    ids.add(product.id);
    this.addedIds.set(ids);
    setTimeout(() => {
      const updated = new Set(this.addedIds());
      updated.delete(product.id);
      this.addedIds.set(updated);
    }, 1500);
  }

  formatPrice(price: number): string {
    return price.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }
}
