import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Factura, Producto, Usuario } from './models/store.models';
import { StoreApiService } from './services/store-api.service';

type StoreView = 'catalog' | 'orders';
type SortOrder = 'featured' | 'priceAsc' | 'priceDesc';
interface CartEntry { productId: number; quantity: number; }
interface NewProductForm { nombre: string; descripcion: string; precio: number; stock: number; imagen: File | null; }

const TIPOS_IMAGEN_ACEPTADOS = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];
const TAMANO_MAXIMO_IMAGEN = 5 * 1024 * 1024;

@Component({
  imports: [CurrencyPipe, DatePipe, DecimalPipe, FormsModule],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App implements OnInit {
  private readonly api = inject(StoreApiService);

  readonly products = signal<Producto[]>([]);
  readonly users = signal<Usuario[]>([]);
  readonly orders = signal<Factura[]>([]);
  readonly loading = signal(true);
  readonly loadError = signal('');
  readonly apiOnline = signal(false);
  readonly view = signal<StoreView>('catalog');
  readonly searchTerm = signal('');
  readonly onlyAvailable = signal(false);
  readonly sortOrder = signal<SortOrder>('featured');
  readonly selectedUser = signal('');
  readonly notice = signal('');
  readonly noticeKind = signal<'success' | 'error'>('success');
  readonly cart = signal<CartEntry[]>(this.restoreCart());
  readonly submitting = signal(false);
  readonly ordersLoading = signal(false);
  readonly ordersError = signal('');
  readonly showProductForm = signal(false);
  readonly showUserForm = signal(false);
  readonly restockProduct = signal<Producto | null>(null);
  readonly savingProduct = signal(false);
  readonly savingUser = signal(false);
  readonly savingStock = signal(false);
  readonly savingImage = signal(false);
  readonly productFormError = signal('');
  readonly productImageError = signal('');
  readonly productImagePreview = signal<string | null>(null);
  readonly userFormError = signal('');
  readonly stockFormError = signal('');

  readonly filteredProducts = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase();
    let result = this.products().filter((product) => {
      const text = `${product.nombre} ${product.descripcion ?? ''}`.toLocaleLowerCase();
      return (!term || text.includes(term)) && (!this.onlyAvailable() || product.stock > 0);
    });
    if (this.sortOrder() === 'priceAsc') result = [...result].sort((a, b) => a.precio - b.precio);
    if (this.sortOrder() === 'priceDesc') result = [...result].sort((a, b) => b.precio - a.precio);
    return result;
  });
  readonly cartDetails = computed(() => this.cart().flatMap((entry) => {
    const product = this.products().find((item) => item.id === entry.productId);
    return product ? [{ product, quantity: entry.quantity, subtotal: product.precio * entry.quantity }] : [];
  }));
  readonly cartCount = computed(() => this.cart().reduce((count, entry) => count + entry.quantity, 0));
  readonly cartTotal = computed(() => this.cartDetails().reduce((total, line) => total + line.subtotal, 0));

  newProduct: NewProductForm = { nombre: '', descripcion: '', precio: 0, stock: 1, imagen: null };
  newUser = { clave: '', nombre: '', codigo: '' };
  restockUnits = 1;

  ngOnInit(): void { this.loadStore(); }

  loadStore(): void {
    this.loading.set(true);
    this.loadError.set('');
    forkJoin({ products: this.api.getProducts(), users: this.api.getUsers() }).subscribe({
      next: ({ products, users }) => {
        this.products.set(products);
        this.users.set(users);
        this.apiOnline.set(true);
        if (!users.some((user) => user.clave === this.selectedUser())) this.selectedUser.set(users[0]?.clave ?? '');
        this.saveCart(this.cart().flatMap((entry) => {
          const product = products.find((item) => item.id === entry.productId);
          return product && product.stock > 0 ? [{ ...entry, quantity: Math.min(entry.quantity, product.stock) }] : [];
        }));
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.apiOnline.set(false);
        this.loading.set(false);
        this.loadError.set(this.errorMessage(error, 'Inicia el backend Spring Boot y vuelve a intentarlo.'));
      },
    });
  }

  showCatalog(): void { this.view.set('catalog'); }
  showOrders(): void { this.view.set('orders'); this.loadOrders(); }

  loadOrders(): void {
    if (!this.selectedUser()) {
      this.orders.set([]);
      this.ordersError.set('');
      this.ordersLoading.set(false);
      return;
    }
    this.ordersLoading.set(true);
    this.ordersError.set('');
    this.api.getOrders(this.selectedUser()).subscribe({
      next: (orders) => { this.orders.set(orders); this.ordersLoading.set(false); },
      error: (error: unknown) => { this.ordersError.set(this.errorMessage(error, 'No se pudieron cargar las compras.')); this.ordersLoading.set(false); },
    });
  }

  selectUser(clave: string): void { this.selectedUser.set(clave); if (this.view() === 'orders') this.loadOrders(); }

  addToCart(product: Producto): void {
    const current = this.cart().find((entry) => entry.productId === product.id)?.quantity ?? 0;
    if (current >= product.stock) { this.showNotice('Ya agregaste todas las unidades disponibles.', 'error'); return; }
    const updated = this.cart().filter((entry) => entry.productId !== product.id);
    updated.push({ productId: product.id, quantity: current + 1 });
    this.saveCart(updated);
    this.showNotice(`${product.nombre} se agregó al carrito.`);
  }

  changeQuantity(productId: number, change: number): void {
    const product = this.products().find((item) => item.id === productId);
    const entry = this.cart().find((item) => item.productId === productId);
    if (!entry || !product) return;
    const quantity = Math.min(product.stock, entry.quantity + change);
    this.saveCart(this.cart().flatMap((item) => item.productId !== productId ? [item] : quantity > 0 ? [{ ...item, quantity }] : []));
  }

  removeFromCart(productId: number): void { this.saveCart(this.cart().filter((entry) => entry.productId !== productId)); }
  clearCart(): void { this.saveCart([]); }

  checkout(): void {
    if (!this.selectedUser()) { this.showNotice('Registra o selecciona un usuario antes de comprar.', 'error'); return; }
    if (this.cart().length === 0) return;
    this.submitting.set(true);
    this.api.createOrder({ usuarioClave: this.selectedUser(), productos: this.cart().map(({ productId, quantity }) => ({ productoId: productId, cantidad: quantity })) }).subscribe({
      next: (order) => {
        this.saveCart([]);
        this.submitting.set(false);
        this.showNotice(`Compra confirmada. Factura #${order.id}.`);
        this.api.getProducts().subscribe({ next: (products) => this.products.set(products) });
        if (this.view() === 'orders') this.loadOrders();
      },
      error: (error: unknown) => { this.submitting.set(false); this.showNotice(this.errorMessage(error, 'No se pudo completar la compra.'), 'error'); },
    });
  }

  openProductForm(): void {
    this.productFormError.set('');
    this.productImageError.set('');
    this.liberarPreview();
    this.newProduct = { nombre: '', descripcion: '', precio: 0, stock: 1, imagen: null };
    this.showProductForm.set(true);
  }

  closeProductForm(): void {
    this.showProductForm.set(false);
    this.liberarPreview();
    this.newProduct.imagen = null;
  }

  /** Maneja la seleccion del archivo de imagen del formulario de producto. */
  selectProductImage(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    this.productImageError.set('');
    if (!archivo) return;

    const error = this.validarImagen(archivo);
    if (error) {
      this.productImageError.set(error);
      input.value = '';
      this.newProduct.imagen = null;
      this.liberarPreview();
      return;
    }

    this.newProduct.imagen = archivo;
    this.liberarPreview();
    this.productImagePreview.set(URL.createObjectURL(archivo));
  }

  saveProduct(): void {
    this.productFormError.set('');
    this.productImageError.set('');

    const imagen = this.newProduct.imagen;
    if (!imagen) {
      this.productImageError.set('Adjunta una imagen del producto para continuar.');
      return;
    }

    this.savingProduct.set(true);
    const { imagen: _imagen, ...datos } = this.newProduct;
    this.api.createProduct({ ...datos }, imagen).subscribe({
      next: (product) => {
        this.products.update((products) => [...products, product]);
        this.savingProduct.set(false);
        this.showProductForm.set(false);
        this.liberarPreview();
        this.newProduct.imagen = null;
        this.showNotice(`${product.nombre} ya está en el catálogo.`);
      },
      error: (error: unknown) => {
        this.savingProduct.set(false);
        this.productFormError.set(this.errorMessage(error, 'No se pudo guardar el producto.'));
      },
    });
  }

  /** Reemplaza la imagen de un producto ya registrado. */
  changeProductImage(product: Producto, event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    input.value = '';
    if (!archivo) return;

    const error = this.validarImagen(archivo);
    if (error) {
      this.showNotice(error, 'error');
      return;
    }

    this.savingImage.set(true);
    this.api.updateProductImage(product, archivo).subscribe({
      next: (updated) => {
        this.products.update((products) => products.map((item) => item.id === updated.id ? updated : item));
        this.savingImage.set(false);
        this.showNotice(`Imagen de ${updated.nombre} actualizada.`);
      },
      error: (error: unknown) => {
        this.savingImage.set(false);
        this.showNotice(this.errorMessage(error, 'No se pudo actualizar la imagen.'), 'error');
      },
    });
  }

  openStockForm(product: Producto): void {
    this.stockFormError.set('');
    this.restockUnits = 1;
    this.restockProduct.set(product);
  }

  closeStockForm(): void {
    if (!this.savingStock()) this.restockProduct.set(null);
  }

  addStock(): void {
    const product = this.restockProduct();
    if (!product || !Number.isInteger(this.restockUnits) || this.restockUnits < 1) {
      this.stockFormError.set('Ingresa una cantidad entera mayor que cero.');
      return;
    }
    this.stockFormError.set('');
    this.savingStock.set(true);
    this.api.addProductStock(product.id, this.restockUnits).subscribe({
      next: (updatedProduct) => {
        this.products.update((products) => products.map((item) => item.id === updatedProduct.id ? updatedProduct : item));
        this.savingStock.set(false);
        this.restockProduct.set(null);
        this.showNotice(`Stock actualizado: ${updatedProduct.nombre} ahora tiene ${updatedProduct.stock} unidades.`);
      },
      error: (error: unknown) => {
        this.savingStock.set(false);
        this.stockFormError.set(this.errorMessage(error, 'No se pudieron agregar las unidades.'));
      },
    });
  }

  openUserForm(): void { this.userFormError.set(''); this.newUser = { clave: '', nombre: '', codigo: '' }; this.showUserForm.set(true); }
  closeUserForm(): void { this.showUserForm.set(false); }

  saveUser(): void {
    this.userFormError.set('');
    this.savingUser.set(true);
    this.api.createUser({ ...this.newUser }).subscribe({
      next: (user) => {
        this.users.update((users) => [...users, user]);
        this.selectedUser.set(user.clave);
        this.savingUser.set(false);
        this.showUserForm.set(false);
        this.showNotice(`Perfil ${user.nombre} creado.`);
        if (this.view() === 'orders') this.loadOrders();
      },
      error: (error: unknown) => { this.savingUser.set(false); this.userFormError.set(this.errorMessage(error, 'No se pudo registrar el usuario.')); },
    });
  }

  resetFilters(): void { this.searchTerm.set(''); this.onlyAvailable.set(false); this.sortOrder.set('featured'); }

  /** URL de la imagen del producto: la de S3 si existe, si no la de respaldo. */
  productImage(product: Producto): string {
    return product.urlImagen || this.fallbackImage(product);
  }

  /** Si la imagen de S3 no carga, se sustituye por la imagen de respaldo. */
  onProductImageError(event: Event, product: Producto): void {
    const img = event.target as HTMLImageElement;
    if (img.dataset['fallback'] === 'true') {
      img.style.visibility = 'hidden';
      return;
    }
    img.dataset['fallback'] = 'true';
    img.src = this.fallbackImage(product);
  }

  scrollToCart(): void { if (this.view() !== 'catalog') this.showCatalog(); document.getElementById('cart')?.scrollIntoView({ behavior: 'smooth', block: 'start' }); }
  clearNotice(): void { this.notice.set(''); }

  private fallbackImage(product: Producto): string {
    const name = `${product.nombre} ${product.descripcion ?? ''}`.toLocaleLowerCase();
    const photos = [
      { match: /reloj|watch/, url: 'photo-1523275335684-37898b6baf30' },
      { match: /zapato|zapatilla|tenis|sneaker/, url: 'photo-1542291026-7eec264c27ff' },
      { match: /aud[ií]fono|headphone|bocina|audio/, url: 'photo-1505740420928-5e560c06d30e' },
      { match: /c[aá]mara|camera|fotograf/, url: 'photo-1516035069371-29a1b244cc32' },
      { match: /mochila|bolso|maleta|bag/, url: 'photo-1553062407-98eeb64c6a62' },
      { match: /laptop|computadora|teclado/, url: 'photo-1496181133206-80ce9b88a853' },
    ];
    const photo = photos.find((item) => item.match.test(name))?.url ?? 'photo-1523275335684-37898b6baf30';
    return `https://images.unsplash.com/${photo}?auto=format&fit=crop&w=760&q=82`;
  }

  private validarImagen(archivo: File): string {
    if (!TIPOS_IMAGEN_ACEPTADOS.includes(archivo.type)) {
      return 'La imagen debe estar en formato JPG, PNG, WEBP o GIF.';
    }
    if (archivo.size > TAMANO_MAXIMO_IMAGEN) {
      return 'La imagen no debe superar 5 MB.';
    }
    return '';
  }

  private liberarPreview(): void {
    const preview = this.productImagePreview();
    if (preview) {
      URL.revokeObjectURL(preview);
      this.productImagePreview.set(null);
    }
  }

  private saveCart(cart: CartEntry[]): void { this.cart.set(cart); localStorage.setItem('nido-cart', JSON.stringify(cart)); }
  private restoreCart(): CartEntry[] {
    try { const value = localStorage.getItem('nido-cart'); return value ? JSON.parse(value) as CartEntry[] : []; }
    catch { return []; }
  }
  private showNotice(message: string, kind: 'success' | 'error' = 'success'): void { this.notice.set(message); this.noticeKind.set(kind); }
  private errorMessage(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as { detail?: string; message?: string } | string | null;
      if (typeof body === 'string' && body) return body;
      if (body && typeof body === 'object') return body.detail ?? body.message ?? fallback;
    }
    return fallback;
  }
}