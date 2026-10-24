import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { CrearFacturaInput, Factura, Producto, ProductoInput, Usuario } from '../models/store.models';

@Injectable({ providedIn: 'root' })
export class StoreApiService {
  private readonly http = inject(HttpClient);
  
  // URL base de la API (configurable por entorno: local o produccion).
  private readonly baseUrl = environment.apiUrl;

  getProducts(): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.baseUrl}/productos`);
  }

  /** Registra un producto con su imagen (multipart: 'producto' + 'imagen'). */
  createProduct(product: ProductoInput, imagen: File): Observable<Producto> {
    return this.http.post<Producto>(`${this.baseUrl}/productos`, this.construirFormulario(product, imagen));
  }

  /** Reemplaza la imagen de un producto existente. */
  updateProductImage(product: Producto, imagen: File): Observable<Producto> {
    const datos: ProductoInput = {
      nombre: product.nombre,
      descripcion: product.descripcion ?? '',
      precio: product.precio,
      stock: product.stock,
    };
    return this.http.put<Producto>(`${this.baseUrl}/productos/${product.id}`, this.construirFormulario(datos, imagen));
  }

  addProductStock(productId: number, unidades: number): Observable<Producto> {
    return this.http.put<Producto>(`${this.baseUrl}/productos/${productId}/stock`, { unidades });
  }

  getUsers(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(`${this.baseUrl}/usuarios`);
  }

  createUser(user: Usuario): Observable<Usuario> {
    return this.http.post<Usuario>(`${this.baseUrl}/usuarios`, user);
  }

  getOrders(userKey: string): Observable<Factura[]> {
    return this.http.get<Factura[]>(`${this.baseUrl}/facturas`, { params: { usuarioClave: userKey } });
  }

  createOrder(order: CrearFacturaInput): Observable<Factura> {
    return this.http.post<Factura>(`${this.baseUrl}/facturas`, order);
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