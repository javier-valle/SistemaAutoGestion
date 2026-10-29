-- Fuente externa que devolvió el nombre (RENIEC vía proveedor, padrón SUNAT, etc.).
-- NULL = no hubo fuente: el nombre lo declaró el cliente o lo escribió el personal.
-- documento_verificado se mantiene por compatibilidad, pero la UI solo afirma
-- "dato de fuente externa" cuando esta columna tiene un valor real.
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS fuente_verificacion VARCHAR(120);
