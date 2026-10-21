-- ============================================================
-- Migracion: desglose de IVA en la tabla facturas
-- ---------------------------------------------------------
-- Agrega el desglose subtotal / iva / iva_porcentaje y deja
-- las facturas existentes consistentes con los items que las
-- componen.
--
-- Ejecutar UNA sola vez sobre una base que ya tenga la tabla
-- 'facturas'. Es idempotente en cuanto al resultado: si las
-- columnas ya existen, MySQL avisa y se puede continuar.
--
-- Las facturas existentes se emitieron sin IVA, por lo que su
-- total era exactamente la suma de sus productos: el subtotal
-- queda igual al total y el IVA en 0. Las facturas nuevas si
-- incluyen el 12%.
-- ============================================================

USE `usuarios`;

-- ------------------------------------------------------------
-- 1. Agregar las columnas
-- ------------------------------------------------------------
ALTER TABLE `facturas`
  ADD COLUMN `subtotal` decimal(12,2) NOT NULL DEFAULT 0,
  ADD COLUMN `iva` decimal(12,2) NOT NULL DEFAULT 0,
  ADD COLUMN `iva_porcentaje` decimal(5,2) NOT NULL DEFAULT 0;

-- ------------------------------------------------------------
-- 2. Reconstruir el desglose de las facturas existentes
--    El total no cambia: subtotal + iva = total
-- ------------------------------------------------------------
UPDATE `facturas`
SET `subtotal` = `total`,
    `iva` = 0,
    `iva_porcentaje` = 12;

-- ------------------------------------------------------------
-- 3. Comprobacion
-- ------------------------------------------------------------
SELECT `id`, `total`, `subtotal`, `iva`, `iva_porcentaje`
FROM `facturas`
ORDER BY `id`;

-- ============================================================
-- Fin de la migracion
-- ============================================================
