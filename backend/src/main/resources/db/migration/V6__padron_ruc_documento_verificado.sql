-- Padrón Reducido RUC (SUNAT, dato público) cargado localmente: consulta de RUC sin API key.
CREATE TABLE IF NOT EXISTS padron_ruc (
    ruc          VARCHAR(11) PRIMARY KEY,
    razon_social VARCHAR(300),
    estado       VARCHAR(50),
    condicion    VARCHAR(50),
    direccion    VARCHAR(500)
);

-- false = el cliente declaró su nombre a mano (no hubo fuente pública que lo confirmara).
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS documento_verificado BOOLEAN NOT NULL DEFAULT TRUE;
