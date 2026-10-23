CREATE TABLE IF NOT EXISTS portal_acceso_log (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT REFERENCES cliente(id) ON DELETE SET NULL,
    documento_hash VARCHAR(64) NOT NULL,
    resultado VARCHAR(20) NOT NULL, -- IDENTIFICADO / FALLIDO / BLOQUEADO
    tipo VARCHAR(10) NOT NULL DEFAULT 'A', -- A=placa, B=OTP
    ip_hash VARCHAR(64),
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_portal_log_cliente ON portal_acceso_log(cliente_id);
CREATE INDEX IF NOT EXISTS idx_portal_log_doc ON portal_acceso_log(documento_hash);
CREATE INDEX IF NOT EXISTS idx_portal_log_fecha ON portal_acceso_log(creado_en);

CREATE TABLE IF NOT EXISTS otp_codigo (
    id BIGSERIAL PRIMARY KEY,
    documento VARCHAR(20) NOT NULL,
    tipo_documento VARCHAR(10) NOT NULL,
    codigo VARCHAR(10) NOT NULL,
    hash_codigo VARCHAR(64) NOT NULL,
    expira_en TIMESTAMP NOT NULL,
    intentos INTEGER DEFAULT 0,
    usado BOOLEAN DEFAULT FALSE,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unq_otp_activo UNIQUE (documento, tipo_documento) WHERE (usado = FALSE AND expira_en > CURRENT_TIMESTAMP)
);
CREATE INDEX IF NOT EXISTS idx_otp_doc ON otp_codigo(documento, tipo_documento);

CREATE TABLE IF NOT EXISTS cita (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    vehiculo_id BIGINT NOT NULL REFERENCES vehiculo(id) ON DELETE RESTRICT,
    fecha TIMESTAMP NOT NULL,
    franja VARCHAR(20) NOT NULL,
    servicio VARCHAR(255),
    comentario VARCHAR(500),
    estado VARCHAR(20) DEFAULT 'SOLICITADA', -- SOLICITADA, CONFIRMADA, CANCELADA, REALIZADA
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cita_cliente ON cita(cliente_id);
CREATE INDEX IF NOT EXISTS idx_cita_fecha ON cita(fecha);

CREATE TABLE IF NOT EXISTS encuesta_satisfaccion (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    orden_trabajo_id BIGINT REFERENCES orden_trabajo(id) ON DELETE SET NULL,
    puntuacion INTEGER NOT NULL CHECK (puntuacion BETWEEN 1 AND 5),
    comentario VARCHAR(1000),
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unq_encuesta_ot UNIQUE (orden_trabajo_id)
);
CREATE INDEX IF NOT EXISTS idx_encuesta_cliente ON encuesta_satisfaccion(cliente_id);

CREATE TABLE IF NOT EXISTS consentimiento (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    politica_version VARCHAR(20) NOT NULL,
    finalidad VARCHAR(50) NOT NULL,
    aceptado BOOLEAN DEFAULT TRUE,
    fecha_aceptacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_hash VARCHAR(64),
    CONSTRAINT unq_consentimiento_cliente_version UNIQUE (cliente_id, politica_version, finalidad)
);
CREATE INDEX IF NOT EXISTS idx_consentimiento_cliente ON consentimiento(cliente_id);

CREATE TABLE IF NOT EXISTS solicitud_arco (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    tipo VARCHAR(20) NOT NULL, -- ACCESO, RECTIFICACION, CANCELACION, OPOSICION, PORTABILIDAD
    descripcion VARCHAR(1000) NOT NULL,
    verificacion VARCHAR(255),
    estado VARCHAR(20) DEFAULT 'RECIBIDA', -- RECIBIDA, EN_PROCESO, ATENDIDA, RECHAZADA
    numero_solicitud VARCHAR(32) UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_solicitud_arco_cliente ON solicitud_arco(cliente_id);
CREATE INDEX IF NOT EXISTS idx_solicitud_arco_estado ON solicitud_arco(estado);

CREATE TABLE IF NOT EXISTS notificacion (
    id BIGSERIAL PRIMARY KEY,
    destinatario_hash VARCHAR(64) NOT NULL,
    canal VARCHAR(20) NOT NULL DEFAULT 'WHATSAPP', -- WHATSAPP, EMAIL, SMS
    plantilla VARCHAR(100) NOT NULL,
    contenido VARCHAR(2000),
    estado VARCHAR(20) DEFAULT 'PENDIENTE', -- PENDIENTE, ENVIADA, FALLIDA, ENTREGADA
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notificacion_destinatario ON notificacion(destinatario_hash);
CREATE INDEX IF NOT EXISTS idx_notificacion_estado ON notificacion(estado);
