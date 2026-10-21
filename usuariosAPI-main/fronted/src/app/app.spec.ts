import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { App } from './app';
import { Factura, Producto, Usuario } from './models/store.models';
import { StoreApiService } from './services/store-api.service';

const USUARIO: Usuario = { clave: '1', nombre: 'Elvis Pres', codigo: 'A-01' };

const FACTURA: Factura = {
  id: 6,
  usuarioClave: '1',
  usuario: USUARIO,
  fecha: '2026-10-20T22:38:23',
  subtotal: 16,
  iva: 1.92,
  ivaPorcentaje: 12,
  total: 17.92,
  productos: [
    { productoId: 6, nombreProducto: 'Manzana', cantidad: 2, precioUnitario: 3, subtotal: 6 },
    { productoId: 7, nombreProducto: 'Pera', cantidad: 4, precioUnitario: 2.5, subtotal: 10 },
  ],
};

describe('App', () => {
  beforeEach(async () => {
    // Se sustituye el servicio real para no depender de un backend en las pruebas.
    TestBed.overrideProvider(StoreApiService, {
      useValue: {
        getProducts: () => of([] as Producto[]),
        getUsers: () => of([USUARIO]),
        getOrders: () => of([FACTURA]),
        createProduct: () => of({} as Producto),
        createUser: () => of(USUARIO),
        createOrder: () => of(FACTURA),
        addProductStock: () => of({} as Producto),
        updateProductImage: () => of({} as Producto),
      },
    });

    await TestBed.configureTestingModule({
      imports: [App],
    })
      .compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Catálogo');
  });

  describe('imagen del producto', () => {
    const base: Producto = {
      id: 1,
      nombre: 'Reloj Clasico',
      descripcion: 'Caja de acero',
      precio: 249.5,
      stock: 3,
      urlImagen: null,
    };

    it('usa la imagen de S3 cuando el producto tiene urlImagen', () => {
      const fixture = TestBed.createComponent(App);
      const app = fixture.componentInstance;
      const conImagen: Producto = {
        ...base,
        urlImagen: 'https://bucket.s3.us-east-2.amazonaws.com/productos/abc.png',
      };

      expect(app.productImage(conImagen)).toBe('https://bucket.s3.us-east-2.amazonaws.com/productos/abc.png');
    });

    it('recurre a la imagen de respaldo cuando no hay urlImagen', () => {
      const fixture = TestBed.createComponent(App);
      const app = fixture.componentInstance;

      expect(app.productImage(base)).toContain('images.unsplash.com');
    });
  });

  describe('seccion mis compras', () => {
    async function abrirFacturas(): Promise<HTMLElement> {
      const fixture = TestBed.createComponent(App);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.componentInstance.showOrders();
      await fixture.whenStable();
      fixture.detectChanges();
      return fixture.nativeElement as HTMLElement;
    }

    it('muestra los datos del cliente de la factura', async () => {
      const compiled = await abrirFacturas();

      const cliente = compiled.querySelector('.order-customer');
      expect(cliente).toBeTruthy();
      expect(cliente?.textContent).toContain('Elvis Pres');
      expect(cliente?.textContent).toContain('A-01');
    });

    it('muestra el desglose de subtotal, IVA y total', async () => {
      const compiled = await abrirFacturas();

      const totales = compiled.querySelector('.order-totals');
      expect(totales).toBeTruthy();
      expect(totales?.textContent).toContain('Subtotal');
      expect(totales?.textContent).toContain('IVA (12%)');
      expect(totales?.textContent).toContain('Total');
      expect(totales?.textContent).toContain('Q16.00');
      expect(totales?.textContent).toContain('Q1.92');
      expect(totales?.textContent).toContain('Q17.92');
    });

    it('conserva el desglose que devuelve la API', async () => {
      const fixture = TestBed.createComponent(App);
      fixture.detectChanges();
      await fixture.whenStable();
      fixture.componentInstance.showOrders();
      await fixture.whenStable();

      const factura = fixture.componentInstance.orders()[0];
      expect(factura.subtotal).toBe(16);
      expect(factura.iva).toBe(1.92);
      expect(factura.ivaPorcentaje).toBe(12);
      expect(factura.total).toBe(17.92);
      expect(factura.usuario.nombre).toBe('Elvis Pres');
    });
  });
});
