-- Solicitud que crea el cliente desde el portal. No es una cotización con
-- precios: es un aviso para que el taller revise el vehículo y elabore la
-- cotización real en recepción/diagnóstico.
CREATE TABLE IF NOT EXISTS solicitud_cotizacion (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    vehiculo_id BIGINT NOT NULL REFERENCES vehiculo(id) ON DELETE RESTRICT,
    descripcion TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE, ATENDIDA, RECHAZADA
    nota_taller VARCHAR(500),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_solicitud_cot_cliente ON solicitud_cotizacion(cliente_id);
CREATE INDEX IF NOT EXISTS idx_solicitud_cot_estado ON solicitud_cotizacion(estado);
CREATE INDEX IF NOT EXISTS idx_solicitud_cot_vehiculo ON solicitud_cotizacion(vehiculo_id);

-- Una sola solicitud pendiente por vehículo: evita que el cliente duplique el aviso.
CREATE UNIQUE INDEX IF NOT EXISTS unq_solicitud_cot_pendiente
    ON solicitud_cotizacion(vehiculo_id) WHERE estado = 'PENDIENTE';
