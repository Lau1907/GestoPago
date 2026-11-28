-- Migración V5: Agregar columnas id_transaccion_proveedor y codigo_upc a transacciones_pagos

ALTER TABLE transacciones_pagos ADD COLUMN IF NOT EXISTS id_transaccion_proveedor VARCHAR(50);
ALTER TABLE transacciones_pagos ADD COLUMN IF NOT EXISTS codigo_upc VARCHAR(50);
