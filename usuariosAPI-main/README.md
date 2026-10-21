# usuariosAPI

API REST de productos, usuarios y facturas, con almacenamiento de imagenes de
producto en AWS S3. Incluye un frontend Angular en `fronted/`.

## Estructura

```
usuariosAPI-main/
├── .env.example            Plantilla de variables de entorno
├── pom.xml                 Configuracion de Maven y SDK de AWS
├── sql/
│   ├── estructura_facturacion.sql   Script de creacion de la base de datos
│   └── migracion_iva.sql            Agrega el desglose de IVA a facturas existentes
├── src/main/java/com/example/practica_api_rest/
│   ├── config/             AwsS3Config, WebConfig (CORS)
│   ├── controllers/        Producto, Usuario, Factura, GlobalExceptionHandler
│   ├── dtos/               Objetos de transferencia
│   ├── exceptions/         Jerarquia de errores de la API
│   ├── models/             Entidades JPA
│   ├── repositories/       Repositorios Spring Data
│   └── services/           Logica de negocio, S3Service
└── fronted/                Aplicacion Angular
```

## Requisitos

- Java 17 o superior
- Node.js 20 o superior
- MySQL 8 o MariaDB 10.4 (por defecto en el puerto 3307)
- Un bucket de AWS S3 con permiso de lectura publica de los objetos

## Configuracion

### 1. Base de datos

Ejecuta `sql/estructura_facturacion.sql` en phpMyAdmin o por consola. El
script crea la base `usuarios` y sus tablas. Si la base ya existe, no hace
falta ejecutarlo: `spring.jpa.hibernate.ddl-auto=update` mantiene el esquema
al dia (por ejemplo, la columna `productos.url_imagen`).

### 2. Variables de entorno

Copia `.env.example` como `.env` y completa los valores:

```bash
cp .env.example .env
```

Spring Boot lee ese archivo automaticamente. Tambien puedes definir las
variables como variables de entorno del sistema, que tienen prioridad sobre
el `.env`.

| Variable | Descripcion | Valor por defecto |
|---|---|---|
| `DB_URL` | URL de conexion MySQL | `jdbc:mysql://localhost:3307/usuarios?...` |
| `DB_USERNAME` | Usuario de la base | `root` |
| `DB_PASSWORD` | Contrasena de la base | *(vacio)* |
| `AWS_ACCESS_KEY_ID` | Credencial de acceso | *(vacio)* |
| `AWS_SECRET_ACCESS_KEY` | Clave secreta | *(vacio)* |
| `AWS_SESSION_TOKEN` | Solo para credenciales temporales | *(vacio)* |
| `AWS_REGION` | Region del bucket | `us-east-2` |
| `AWS_S3_BUCKET` | Bucket de las imagenes | `apirest-productos-imagenes-proyecto` |
| `CORS_ORIGINS` | Origenes permitidos del frontend | `http://localhost:4200` |
| `IVA_PORCENTAJE` | Porcentaje de IVA de las facturas | `12` |

Si `AWS_ACCESS_KEY_ID` y `AWS_SECRET_ACCESS_KEY` quedan vacias, la aplicacion
usa la cadena estandar de credenciales de AWS (perfil de `~/.aws/credentials`
o el rol de la instancia EC2). La aplicacion arranca aunque falten las
credenciales; el error aparece unicamente al subir un archivo.

### 3. Base de datos en la nube (AWS RDS)

La `DB_URL` puede apuntar a una instancia RDS en vez de MySQL local. **La URL
debe incluir el nombre de la base de datos**: sin el, la conexion se abre
igual pero toda consulta falla con `No database selected` y la API responde
`500 Error interno del servidor`.

```properties
# En la nube (AWS RDS)
DB_URL=jdbc:mysql://database-2.c1okkak04l16.us-east-2.rds.amazonaws.com/usuarios?serverTimezone=UTC

# En local (MySQL / MariaDB)
DB_URL=jdbc:mysql://localhost:3307/usuarios?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

Contra RDS el cifrado TLS va activo por defecto (Connector/J 9 lo negotiate
solo), por eso no hace falta `useSSL=false`. `serverTimezone=UTC` mantiene la
zona horaria alineada con el servidor.

Para una instancia RDS nueva, crea primero el esquema:

```bash
mysql -h <host-rds> -u <usuario> -p usuarios < sql/estructura_facturacion.sql
```

Y si ya existian facturas sin el desglose de IVA, aplica ademas la migracion:

```bash
mysql -h <host-rds> -u <usuario> -p usuarios < sql/migracion_iva.sql
```

### 4. Permisos del bucket

El bucket debe permitir la lectura publica de los objetos, para que las URLs
guardadas en la base se puedan mostrar en el navegador:

```json
{
  "Version": "2012-10-10",
  "Statement": [{
    "Sid": "PublicReadGetObject",
    "Effect": "Allow",
    "Principal": "*",
    "Action": "s3:GetObject",
    "Resource": "arn:aws:s3:::apirest-productos-imagenes-proyecto/*"
  }]
}
```

El usuario de AWS necesita `s3:PutObject`, `s3:GetObject` y `s3:DeleteObject`
sobre ese bucket.

## Ejecucion

Backend:

```bash
./mvnw spring-boot:run
```

Frontend:

```bash
cd fronted
npm install
npm start
```

El frontend corre en `http://localhost:4200` y redirige las llamadas a la API
por el proxy definido en `fronted/proxy.conf.json`.

## API

| Metodo | Ruta | Descripcion |
|---|---|---|
| `GET` | `/productos` | Lista los productos |
| `GET` | `/productos/{id}` | Busca un producto |
| `POST` | `/productos` | Registra un producto **con imagen** (multipart) |
| `PUT` | `/productos/{id}` | Actualiza un producto (JSON o multipart) |
| `PUT` | `/productos/{id}/stock` | Suma unidades al inventario |
| `DELETE` | `/productos/{id}` | Elimina un producto |
| `GET` | `/usuarios` | Lista los usuarios |
| `POST` | `/usuarios` | Crea un usuario |
| `GET` | `/facturas` | Lista las facturas (filtro por `usuarioClave`) |
| `POST` | `/facturas` | Crea una factura |

### Registrar un producto con imagen

La imagen es obligatoria. La solicitud es `multipart/form-data` con dos partes:
`producto` (JSON) e `imagen` (archivo).

```bash
curl -X POST http://localhost:8080/productos \
  -F 'producto={"nombre":"Reloj","precio":249.50,"stock":10};type=application/json' \
  -F 'imagen=@reloj.png;type=image/png'
```

Se aceptan imagenes JPG, PNG, WEBP y GIF de hasta 5 MB. El tipo se comprueba
con la firma binaria del archivo, no con el content type declarado.

La respuesta incluye `urlImagen`, la URL publica del objeto en S3.

### Reemplazar la imagen de un producto

```bash
curl -X PUT http://localhost:8080/productos/1 \
  -F 'producto={"nombre":"Reloj","precio":299.00,"stock":8};type=application/json' \
  -F 'imagen=@reloj-nuevo.png;type=image/png'
```

El objeto anterior se elimina de S3. Un `PUT` en JSON conserva la imagen
actual.

## Facturas e IVA

Cada factura guarda el desglose del importe:

- `subtotal`: suma de los productos antes de IVA
- `iva`: `subtotal × IVA_PORCENTAJE`, redondeado a dos decimales
- `iva_porcentaje`: la tasa que se aplico a esa factura
- `total`: `subtotal + iva`

La tasa se configura con `app.iva.porcentaje` (12 por defecto) y se guarda en
cada factura, de modo que un cambio de tasa no altera el historico.

La respuesta incluye ademas los datos del cliente en el objeto `usuario`:

```json
{
  "id": 6,
  "usuarioClave": "1",
  "usuario": { "clave": "1", "nombre": "Elvis Pres", "codigo": "A-01" },
  "fecha": "2026-10-20T22:38:23",
  "subtotal": 16.00,
  "iva": 1.92,
  "ivaPorcentaje": 12.00,
  "total": 17.92,
  "productos": [ ... ]
}
```

### Migrar una base de datos existente

Si la tabla `facturas` ya existia sin las columnas del IVA, ejecuta una sola vez:

```bash
mysql -u root -p usuarios < sql/migracion_iva.sql
```

El script agrega `subtotal`, `iva` e `iva_porcentaje` y deja las facturas
anteriores con `subtotal = total` e `iva = 0`, porque se emitieron cuando el
IVA todavia no se aplicaba. Las facturas nuevas si incluyen el 12%.

## Errores

Todas las respuestas de error usan el mismo formato:

```json
{ "detail": "nombre es obligatorio", "message": "nombre es obligatorio" }
```

| Codigo | Significado |
|---|---|
| 400 | Solicitud invalida (validacion, imagen ausente o formato incorrecto) |
| 404 | El recurso no existe |
| 409 | Conflicto (duplicado o recurso en uso) |
| 502 | Fallo al comunicarse con AWS S3 |

## Pruebas

```bash
./mvnw test          # backend
cd fronted && npm test   # frontend
```
