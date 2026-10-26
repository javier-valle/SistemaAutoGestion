# Políticas y cumplimiento (Fase 3) — ADVERTENCIA LEGAL

> **IMPORTANTE:** Todos los textos de políticas en este repositorio son **borradores técnicos** para un sistema académico. **Deben ser revisados por un abogado antes de ser usados en producción.** No presentan validez legal definitiva.

## Marco normativo considerado (Perú)

- **Ley N.º 29733** y **Reglamento DS N.º 016-2024-JUS** (vigente desde 30-03-2025): protección de datos personales, derechos ARCO ampliados (incluye portabilidad), oficial de datos personales, medidas de seguridad y notificación de incidentes.
- **Ley N.º 29571**, Código de Protección y Defensa del Consumidor: información clara, libro de reclamaciones.
- Comprobantes: el sistema actual es académico (no firma XML ni envía a SUNAT). En producción requiere integración con OSE/PSE.

## Datos desconocidos o por definir

Todos los campos marcados como `[TODO]` o `<!-- TODO -->` deben ser completados por el responsable del taller antes de cualquier uso en producción:

- RAZON_SOCIAL, RUC_TALLER, DIRECCION, TELEFONO_WHATSAPP, CORREO_CONTACTO, CORREO_ARCO, OFICIAL_DATOS.
- Inscripción del banco de datos en la autoridad competente (`[TODO verificar]`).
- Plazos exactos de atención ARCO (`[TODO verificar plazos vigentes]`).
- Requisitos vigentes del libro de reclamaciones virtual (`[TODO verificar requisitos vigentes]`).
- Plazos y costos de custodia de vehículos no recogidos (`[TODO validar con abogado]`).

## Funcionalidad técnica implementada (borrador)

- Consentimientos versionados (`consentimiento`).
- Solicitudes ARCO (`solicitud_arco`) con número, plazo y estado.
- Notificaciones con opt-in (`notificacion`).
- Retención programada por tipo de dato.
- Hash de IP (no IP en claro) en logs (`portal_acceso_log`).

Nada de esto sustituye la revisión legal ni los requisitos formales que exijan las autoridades competentes.
