# Políticas de datos personales — estado del proyecto

Documento de trabajo sobre el tratamiento de datos personales en AutoGestión. Los textos de
política que muestra el portal son **borradores técnicos** de un sistema académico: describen
lo que el software hace, no sustituyen la redacción legal ni la inscripción de la base de
datos ante la autoridad competente.

> Antes de operar con clientes reales, estos textos deben ser revisados por un abogado y
> completados los parámetros de la sección 4. Mientras eso no ocurra, el sistema no debe
> usarse con datos de personas ajenas al taller de demostración.

## 1. Marco normativo considerado

| Norma | Qué obliga en este sistema |
| :-- | :-- |
| Ley N.º 29733, Protección de Datos Personales | Finalidad declarada, consentimiento, derechos del titular y medidas de seguridad |
| Reglamento DS N.º 016-2024-JUS (vigente desde el 30-03-2025) | Derechos ARCO ampliados —incluida la portabilidad—, oficial de datos personales y notificación de incidentes |
| Ley N.º 29571, Código de Protección y Defensa del Consumidor | Información clara al cliente y libro de reclamaciones |
| Normativa de comprobantes de pago | El comprobante actual es académico: sin firma digital ni envío a SUNAT |

## 2. Qué implementa el sistema

| Elemento | Dónde vive | Qué hace |
| :-- | :-- | :-- |
| Consentimiento versionado | tabla `consentimiento` | Guarda la versión del texto aceptada y su fecha |
| Solicitudes ARCO | tabla `solicitud_arco` | Estructura para registrar tipo, plazo y estado; el trámite se atiende hoy por el correo publicado, sin formulario en el portal |
| Acceso al portal auditado | tabla `portal_acceso_log` | Registra el resultado del intento con documento e IP en hash, nunca en claro |
| Notificaciones con opt-in | tabla `notificacion` | El cliente decide si recibe avisos |
| Retención por tipo de dato | tarea programada | Elimina lo que supera el plazo definido |
| Verificación de documento | tablas `padron_ruc` y `cliente` | Guarda la fuente de la consulta y si el documento quedó verificado |

El documento de identidad se almacena normalizado y su consulta pública está limitada por
`ConsultaDocumentoRateLimit`, de modo que un tercero no pueda recorrer el padrón probando
números.

## 3. Textos que ve el cliente

El portal publica la política de privacidad, los términos de uso y el libro de reclamaciones
en `frontend/portal/politicas.html`. La política incluye finalidad del tratamiento, datos
recogidos, plazo de conservación, encargados, derechos del titular y el canal para ejercerlos.

## 4. Parámetros pendientes de completar

Estos valores son del taller, no del código, y hoy aparecen como marcadores en la
configuración:

- Razón social, RUC, dirección, teléfono de contacto y correos de contacto y de ARCO.
- Nombre del oficial de datos personales designado.
- Inscripción de la base de datos ante la autoridad competente.
- Plazos de atención de las solicitudes ARCO, según la norma vigente.
- Requisitos del libro de reclamaciones virtual.
- Plazos y costos de custodia de vehículos no recogidos.

Se configuran en `.env` (ver `.env.example`); ninguno debe quedar escrito directamente en el
código ni en el repositorio.

## 5. Recomendaciones antes de producción

1. Sustituir los borradores por textos revisados por el área legal del taller.
2. Definir por escrito el plazo de conservación de cada tipo de dato y dejarlo configurado.
3. Nombrar al oficial de datos y publicar su canal de contacto.
4. Establecer el procedimiento de notificación de incidentes.
5. Integrar la facturación electrónica con un OSE/PSE si el comprobante debe tener validez
   tributaria.
