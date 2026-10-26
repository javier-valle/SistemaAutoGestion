# Plan de ejecución AutoGestión (4 fases)

## Parámetros (no proporcionados → TODO en todo)
- RAZON_SOCIAL = [TODO - definir]
- RUC_TALLER = [TODO - definir]
- DIRECCION = [TODO - definir]
- TELEFONO_WHATSAPP = [TODO - definir]
- CORREO_CONTACTO = [TODO - definir]
- CORREO_ARCO = [TODO - definir]
- OFICIAL_DATOS = [TODO - definir]
- SPLASH_SCREEN = SI (asumo, por falta de NO explícito) → textos vacíos con TODO
- SPLASH_TITULO = [TODO]
- SPLASH_SUBTITULO = [TODO]
- SPLASH_IMAGEN = [TODO]
- MODO_PORTAL = A+B (asumo ambos modos disponibles)

## Fase 1: Portal de Cliente (en curso)
Objetivo: seguridad, API `/api/public/portal/*`, migración V5 (portal_acceso_log, otp_codigo, cita, encuesta_satisfaccion, consentimiento, solicitud_arco, notificacion), páginas HTML en `frontend/portal/`, sistema de diseño Bl4z3 base.

Riesgos conocidos:
- No hay datos legales definitivos → todo marcado con advertencia.
- No se integrará WhatsApp real ni proveedor de OTP sin credenciales; se usará interfaz `Notificador` con `LogNotificador` por defecto.
- No se edita ninguna migración existente (V1-V4). Nueva: V5.

## Fase 2: Ventanas internas nuevas
11 ventanas nuevas (agenda, garantías, caja, checklist, auditoría, cuentas por cobrar, notificaciones, productividad, TV, compras, políticas admin). Cada una: página + backend + migración + permisos.

## Fase 3: Políticas y cumplimiento
Borradores técnicos con advertencia de revisión legal. Páginas en `frontend/pages/politicas/` y `frontend/portal/politicas.html`. Funcionalidad de consentimiento, ARCO, portabilidad, retención.

## Fase 4: Rediseño Bl4z3
Reemplazo completo del CSS y reconstrucción de todas las pantallas (existentes + nuevas) con layout 100% fluido, tokens, componentes, accesibilidad. Consolidación de CSS en capas mantenibles.
