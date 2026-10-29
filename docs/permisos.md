# Matriz de permisos — AutoGestión

Roles del sistema: **ADMIN**, **RECEPCIONISTA**, **MECÁNICO**, **ALMACENERO** y el acceso
del **cliente** por el portal público.

La autorización real se aplica en el backend con `@RolesAllowed` en cada controlador y se
completa con `JwtAuthFilter`, que valida el token en cada petición. El menú del frontend
oculta las páginas que no corresponden al rol, pero es solo una ayuda visual: ocultar un
botón no protege nada, y por eso el backend vuelve a comprobar el rol en cada endpoint.

## Módulos del personal

| Módulo / acción | ADMIN | RECEPCIONISTA | MECÁNICO | ALMACENERO |
| :-- | :-- | :-- | :-- | :-- |
| Panel e indicadores | Sí | Sí | Solo lo suyo | Sí |
| Clientes y vehículos | Sí | Sí | Consulta | No |
| Recepción | Sí | Sí (registra) | Consulta | No |
| Diagnóstico y cotización | Sí | Sí (crea y aprueba) | Sí (diagnostica) | No |
| Órdenes de trabajo | Sí | Sí (crea) | Sí (avanza estado, consume repuestos) | No |
| Inventario y movimientos | Sí | Consulta | Consulta | Sí (compras, mermas, ajustes) |
| Proveedores | Sí | No | No | Sí |
| Catálogo de servicios | Sí | Sí | Sí | Sí |
| Pago y entrega | Sí | Sí (cobra y entrega) | No | No |
| Comprobantes | Sí (emite y anula) | Emite y descarga | No | No |
| Gastos | Sí | No | No | No |
| Reportes | Todos | Clientes e ingresos | No | Inventario |
| Exportación consolidada | Sí | No | No | No |
| Equipo y claves | Sí | No | No | No |
| Listado de mecánicos | Sí | Sí | Consulta | No |
| Solicitudes de cotización del portal | Sí | Sí (atiende) | Sí (consulta) | No |

## Reportes, por pestaña

| Pestaña | Roles con acceso |
| :-- | :-- |
| Resumen | ADMIN |
| Clientes | ADMIN, RECEPCIONISTA |
| Ingresos | ADMIN |
| Inventario | ADMIN, ALMACENERO |
| Gastos | ADMIN |
| Resultado | ADMIN |
| Mecánicos | ADMIN |
| Exportación consolidada | ADMIN |

## Portal del cliente

| Acción | Requisito |
| :-- | :-- |
| Consultar el estado de un vehículo | Documento del titular; sin cuenta |
| Registrarse y ver historial | Documento + código de verificación enviado al registrar |
| Aprobar o rechazar una cotización | Sesión del portal |
| Solicitar una cotización | Sesión del portal y vehículo propio sin solicitud pendiente |
| Descargar comprobantes | Sesión del portal |
| Ejercer derechos ARCO | Correo de contacto publicado en la política; el portal todavía no tiene formulario |

Los endpoints `/api/public/portal/**` son accesibles sin sesión porque el cliente no tiene
usuario del sistema, y están limitados por `ConsultaDocumentoRateLimit` para impedir la
enumeración de documentos. Los endpoints `/api/portal/**` exigen el token del portal, que
`PortalTokenService` emite y valida con vigencia corta.
