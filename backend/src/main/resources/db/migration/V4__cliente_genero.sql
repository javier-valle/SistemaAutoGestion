-- V4: género opcional del cliente (MASCULINO/FEMENINO/OTROS).
-- Solo personaliza la vista del cliente; la identidad sigue siendo
-- (tipo_documento, documento). IDEMPOTENTE y corre igual en
-- PostgreSQL 16 y en H2 en modo PostgreSQL.
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS genero VARCHAR(20);

ALTER TABLE cliente DROP CONSTRAINT IF EXISTS chk_cliente_genero;
ALTER TABLE cliente ADD CONSTRAINT chk_cliente_genero
  CHECK (genero IS NULL OR genero IN ('MASCULINO', 'FEMENINO', 'OTROS'));
