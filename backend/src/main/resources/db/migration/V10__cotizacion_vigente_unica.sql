-- Un diagnóstico puede renegociarse, pero no tener dos cotizaciones vivas a la vez.
-- Se usa una columna marcador en lugar de un índice parcial porque H2 (tests) no
-- soporta CREATE INDEX ... WHERE; el efecto es el mismo: NULL no colisiona.
ALTER TABLE cotizacion ADD COLUMN IF NOT EXISTS diagnostico_vigente_id BIGINT;

-- Repara duplicados heredados: conserva la APROBADA más reciente y rechaza el resto.
UPDATE cotizacion SET estado = 'RECHAZADA'
WHERE id IN (
    SELECT id FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY diagnostico_id
                   ORDER BY CASE WHEN estado = 'APROBADA' THEN 0 ELSE 1 END, fecha DESC, id DESC
               ) AS rn
        FROM cotizacion
        WHERE estado IN ('PENDIENTE', 'EN_DIAGNOSTICO', 'APROBADA')
    ) t
    WHERE rn > 1
);

UPDATE cotizacion SET diagnostico_vigente_id = diagnostico_id
WHERE estado IN ('PENDIENTE', 'EN_DIAGNOSTICO', 'APROBADA');

UPDATE cotizacion SET diagnostico_vigente_id = NULL
WHERE estado NOT IN ('PENDIENTE', 'EN_DIAGNOSTICO', 'APROBADA');

CREATE UNIQUE INDEX IF NOT EXISTS ux_cotizacion_diagnostico_vigente
    ON cotizacion (diagnostico_vigente_id);
