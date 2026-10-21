-- ============================================================
-- Script SQL para phpMyAdmin / MySQL
-- Sistema de facturación - Base de datos: usuarios
-- Coincide con application.properties (puerto 3307) y con las
-- entidades JPA del proyecto usuariosAPI-main
--
-- Nota: spring.jpa.hibernate.ddl-auto=update mantiene el esquema
-- sincronizado, por lo que este script solo es necesario para crear
-- la base de datos desde cero.
-- ============================================================

CREATE DATABASE IF NOT EXISTS `usuarios`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_spanish_ci;

USE `usuarios`;

-- ------------------------------------------------------------
-- Tabla: usuarios  (coincide con entidad Usuario)
-- ------------------------------------------------------------
CREATE TABLE `usuarios` (
  `clave` varchar(11) NOT NULL,
  `codigo` varchar(11) DEFAULT NULL,
  `nombre` varchar(11) DEFAULT NULL,
  PRIMARY KEY (`clave`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ------------------------------------------------------------
-- Tabla: productos  (coincide con entidad Producto)
-- url_imagen guarda la URL publica del objeto en AWS S3
-- ------------------------------------------------------------
CREATE TABLE `productos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(120) NOT NULL,
  `descripcion` varchar(500) DEFAULT NULL,
  `precio` decimal(10,2) NOT NULL,
  `stock` int NOT NULL,
  `url_imagen` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ------------------------------------------------------------
-- Tabla: facturas  (coincide con entidad Factura)
-- subtotal: suma de los productos antes de IVA
-- iva: impuesto aplicado, iva_porcentaje: tasa usada en esa factura
-- total: subtotal + iva
-- ------------------------------------------------------------
CREATE TABLE `facturas` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fecha` datetime NOT NULL,
  `subtotal` decimal(12,2) NOT NULL,
  `iva` decimal(12,2) NOT NULL,
  `iva_porcentaje` decimal(5,2) NOT NULL,
  `total` decimal(12,2) NOT NULL,
  `usuario_clave` varchar(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `usuario_clave` (`usuario_clave`),
  CONSTRAINT `facturas_ibfk_1` FOREIGN KEY (`usuario_clave`) REFERENCES `usuarios` (`clave`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ------------------------------------------------------------
-- Tabla: detalles_factura  (coincide con entidad DetalleFactura)
-- ------------------------------------------------------------
CREATE TABLE `detalles_factura` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `factura_id` bigint NOT NULL,
  `producto_id` bigint NOT NULL,
  `cantidad` int NOT NULL,
  `precio_unitario` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `factura_id` (`factura_id`),
  KEY `producto_id` (`producto_id`),
  CONSTRAINT `detalles_factura_ibfk_1` FOREIGN KEY (`factura_id`) REFERENCES `facturas` (`id`),
  CONSTRAINT `detalles_factura_ibfk_2` FOREIGN KEY (`producto_id`) REFERENCES `productos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_spanish_ci;

-- ============================================================
-- Fin del script
-- ============================================================
