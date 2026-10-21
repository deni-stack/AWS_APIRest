import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CrearFacturaInput, Factura, Producto, ProductoInput, Usuario } from '../models/store.models';

@Injectable({ providedIn: 'root' })
export class StoreApiService {
  private readonly http = inject(HttpClient);

  getProducts(): Observable<Producto[]> {
    return this.http.get<Producto[]>('/productos');
  }

  /** Registra un producto con su imagen (multipart: 'producto' + 'imagen'). */
  createProduct(product: ProductoInput, imagen: File): Observable<Producto> {
    return this.http.post<Producto>('/productos', this.construirFormulario(product, imagen));
  }

  /** Reemplaza la imagen de un producto existente. */
  updateProductImage(product: Producto, imagen: File): Observable<Producto> {
    const datos: ProductoInput = {
      nombre: product.nombre,
      descripcion: product.descripcion ?? '',
      precio: product.precio,
      stock: product.stock,
    };
    return this.http.put<Producto>(`/productos/${product.id}`, this.construirFormulario(datos, imagen));
  }

  addProductStock(productId: number, unidades: number): Observable<Producto> {
    return this.http.put<Producto>(`/productos/${productId}/stock`, { unidades });
  }

  getUsers(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>('/usuarios');
  }

  createUser(user: Usuario): Observable<Usuario> {
    return this.http.post<Usuario>('/usuarios', user);
  }

  getOrders(userKey: string): Observable<Factura[]> {
    return this.http.get<Factura[]>('/facturas', { params: { usuarioClave: userKey } });
  }

  createOrder(order: CrearFacturaInput): Observable<Factura> {
    return this.http.post<Factura>('/facturas', order);
  }

  /**
   * Arma el cuerpo multipart. La parte 'producto' se envia como JSON
   * (Blob con content type application/json) y la parte 'imagen' como archivo.
   */
  private construirFormulario(product: ProductoInput, imagen: File): FormData {
    const datos = new FormData();
    datos.append('producto', new Blob([JSON.stringify(product)], { type: 'application/json' }));
    datos.append('imagen', imagen, imagen.name);
    return datos;
  }
}
