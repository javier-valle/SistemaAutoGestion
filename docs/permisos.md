# Matriz de permisos

| Función / Rol | ADMIN | MECANICO | RECEPCIONISTA | ALMACENERO | CLIENTE (Portal) |
|---|---|---|---|---|---|
| Dashboard | Sí | Solo indicadores propios | Sí | Sí | Solo lectura de OT |
| Recepción | Sí | No | Sí | No | No |
| Cotización | Sí | Lectura (asignación) | Sí (crear/aprobar) | No | Aprobación/rechazo (modo B) |
| Órdenes de trabajo | Sí | Sí (asignación/estado) | Sí (crear) | No | Solo lectura de estado |
| Inventario | Sí | Lectura | Lectura | Sí (movimientos/alertas) | No |
| Pago / Entrega | Sí (emitir/anular) | No | Sí (emitir) | No | Ver comprobantes (modo B) |
| Comprobantes | Sí (emitir/anular) | No | Emitir | No | Descargar (modo B) |
| Equipo | Sí (crear/editar) | No | No | No | No |
| Reportes | Sí | Resumen propio | Sí (clientes/ingresos) | Inventario | No |
| Portal público | No | No | No | No | Consulta (A+B) |
| Agenda / Citas | Sí | Lectura | Sí | No | Solicitar (modo B) |
| Garantías | Sí | Lectura | Sí | No | No |
| Órdenes de compra | Sí | No | No | Sí | No |
| Caja diaria | Sí | No | Apertura/cierre | No | No |
| Checklist / Fotos | Lectura | Lectura | Sí (crear/fotos) | No | No |
| Auditoría | Sí (solo lectura) | No | No | No | No |
| Cuentas por cobrar | Sí | No | Sí (recordatorios) | No | No |
| Notificaciones | Sí (plantillas/cola) | No | Lectura | No | Recibir (opt-in) |
| Productividad | Sí | Solo su rendimiento | No | No | No |
| Vista TV | Sí (token pantalla) | No | No | No | No |
| Políticas (admin) | Sí (publicar) | No | No | No | Ver consentimientos / ARCO |

Nota: La seguridad real siempre está en backend (`@PreAuthorize`, `JwtAuthFilter`). El menú de frontend oculta elementos según rol, pero nunca confía solo en eso.
