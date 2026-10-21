export interface Producto {
  id: number;
  nombre: string;
  descripcion: string | null;
  precio: number;
  stock: number;
  /** URL publica de la imagen del producto en AWS S3. */
  urlImagen: string | null;
}

export interface Usuario {
  clave: string;
  nombre: string;
  codigo: string;
}

export interface ItemFacturaInput {
  productoId: number;
  cantidad: number;
}

export interface CrearFacturaInput {
  usuarioClave: string;
  productos: ItemFacturaInput[];
}

export interface DetalleFactura {
  productoId: number;
  nombreProducto: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface Factura {
  id: number;
  usuarioClave: string;
  /** Datos del cliente al que se emitio la factura. */
  usuario: Usuario;
  fecha: string;
  /** Suma de los productos antes de IVA. */
  subtotal: number;
  /** Importe del IVA aplicado. */
  iva: number;
  /** Porcentaje de IVA que se aplico a esta factura. */
  ivaPorcentaje: number;
  /** Subtotal + IVA. */
  total: number;
  productos: DetalleFactura[];
}

/** Datos del producto que viajan como parte JSON del multipart. */
export interface ProductoInput {
  nombre: string;
  descripcion: string;
  precio: number;
  stock: number;
}