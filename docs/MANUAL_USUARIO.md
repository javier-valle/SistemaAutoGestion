# Manual de usuario — AutoGestión

Uso del sistema por rol. La referencia técnica está en
[GUIA_DESARROLLO.md](GUIA_DESARROLLO.md) y la matriz de permisos en
[permisos.md](permisos.md).

## 1. Acceso

| Perfil | Dirección | Credencial |
| :-- | :-- | :-- |
| Personal del taller | `http://localhost:8081/admin/` | Usuario y contraseña asignados por el administrador |
| Cliente | `http://localhost:8081/` | Documento + código enviado al registrar, o consulta sin cuenta |

Atajos comunes: `Ctrl+K` abre el buscador de páginas y el botón **¿Cómo usar?** inicia el
recorrido guiado de la pantalla en la que estás. Cada pantalla trae un banner de tres pasos
que se puede ocultar.

## 2. Recepcionista

**Recibir un vehículo.** Recepción → busca al cliente por documento, nombre o teléfono. Si
ya existe, elige su tarjeta y su vehículo; si no, créalo con el documento validado. Registra
problema, combustible, kilometraje, daños y accesorios, y confirma. Anota el número de
recepción y continúa en Cotización para el diagnóstico.

**Cotizar.** Elige la recepción pendiente y el mecánico (el sistema propone el menos
ocupado), registra el diagnóstico y agrega servicios y productos; el total se calcula solo.
Genera la cotización y márcala como aprobada cuando el cliente acepte.

**Abrir la orden de trabajo.** Órdenes → elige la cotización aprobada y el mecánico → Crear.
Desde ese momento el mecánico puede avanzar el estado.

**Cobrar y entregar.** Pago y Entrega → Cobrar → Boleta (DNI) o Factura (RUC, razón social y
dirección fiscal) → método de pago → Confirmar. El comprobante se imprime o se descarga en
PDF. Al final, registra la entrega del vehículo.

**Clientes.** Busca y abre la ficha: vehículos, visitas y comprobantes. Desde ahí puedes
iniciar una recepción con el cliente ya cargado.

## 3. Mecánico

Tu panel muestra el saludo con los trabajos pendientes ordenados por prioridad. Cada orden se
avanza con un botón: **Iniciar → Pasar a prueba → Finalizar**. Al finalizar se habilita el
cobro en caja.

Con la orden en proceso o en prueba puedes usar **+ Repuesto** para descontar un producto del
inventario; si no hay stock, el sistema lo bloquea. El inventario se consulta en modo lectura
y las órdenes en el tablero kanban.

## 4. Almacenero

**Stock.** Busca productos y revisa precio, costo y margen; el semáforo señala el stock bajo.

**Compras.** Pestaña Registrar compra → proveedor (o créalo con RUC validado), documento y
líneas con su costo. Al confirmar se actualiza el stock y el último costo.

**Mermas y ajustes.** Tipo, producto, cantidad y motivo. El motivo es obligatorio y queda
registrado con tu usuario.

**Movimientos.** Filtra por tipo o fecha; el motivo aparece en el detalle. En Reportes →
Inventario están el kardex por producto y la valorización.

## 5. Administrador

Incluye todo lo anterior y además:

- **Equipo**: crear, editar y desactivar personal, y restablecer contraseñas. El sistema
  impide desactivar la propia cuenta o dejar la instalación sin ningún administrador.
- **Gastos**: alquiler, servicios y sueldos, por categoría y fecha.
- **Resultado**: utilidad estimada con la fórmula a la vista.
- **Anulación de comprobantes**: exige motivo, deja rastro de auditoría y el número no se
  reutiliza.
- **Reportes**: todas las pestañas, con exportación por tabla a Excel y PDF, y consolidado de varias pestañas.

## 6. Módulo de reportes

### Pestañas y roles

| Pestaña | Qué muestra | Roles |
| :-- | :-- | :-- |
| Resumen | Indicadores del periodo: clientes atendidos, ingresos, comprobantes, inventario | ADMIN |
| Clientes | Clientes atendidos por día y recepciones por estado | ADMIN, RECEPCIONISTA |
| Ingresos | Ingresos por día, resumen de comprobantes y servicios más pedidos | ADMIN |
| Inventario | Stock bajo, consumo, kardex y entradas | ADMIN, ALMACENERO |
| Gastos | Gastos por categoría en el periodo | ADMIN |
| Resultado | Ingresos netos, costo de lo consumido, gastos y utilidad estimada | ADMIN |
| Mecánicos | Órdenes y productividad por mecánico | ADMIN |

Las pestañas que no corresponden al rol no se muestran, y el backend rechaza la consulta
aunque se invoque directamente.

### Rangos de fecha

Los botones **Hoy**, **7 días**, **Mes** y **Mes anterior** fijan el rango; también se pueden
escribir las fechas a mano. Todos los indicadores y gráficos respetan el rango elegido y se
calculan con la zona `America/Lima`. Sin filtro, el sistema toma el mes en curso.

### Exportación

Cada tabla ofrece dos botones: **Excel** y **PDF**. El archivo sale con el mismo diseño de los
reportes (encabezado fijo, importes con formato de moneda, cifras numéricas de verdad y
área de impresión A4), y la exportación trae todas las filas aunque en pantalla se muestren
solo las primeras cien.

La opción **Exportar consolidado** reúne las pestañas seleccionadas en un solo archivo con
portada, y es la que se entrega cuando se pide el reporte del periodo completo. El botón
**PDF / Imprimir** de la cabecera genera una vista de impresión de la pestaña activa.

Los gráficos llevan título y descripción accesibles, así que un lector de pantalla puede
anunciar qué muestra cada uno.

### Lectura de los resultados

El resultado es una **estimación académica**, no un estado financiero contable: no incluye
depreciación, deudas ni obligaciones tributarias. Las mermas y los ajustes negativos se
restan como línea propia para que la diferencia con el conteo físico quede visible.

## 7. Portal del cliente

Con el documento, el cliente consulta el estado de su vehículo sin crear cuenta. Al
registrarse con el código de verificación obtiene además el panel con sus vehículos, el
detalle de sus órdenes, las cotizaciones para aprobar o rechazar y la solicitud de
cotización.

La solicitud desde el portal no es una cotización con precios: es un aviso para que el taller
revise el vehículo y elabore la cotización en recepción. Mientras una solicitud sigue
pendiente, el mismo vehículo no admite otra, y un cliente no puede acumular más de tres.

Las pantallas de comprobantes, historial, citas y encuesta están maquetadas pero todavía no
tienen endpoint: muestran un aviso en lugar de un formulario que no guarda nada. La política
de datos personales sí es definitiva en cuanto al canal de contacto, que hoy es el correo
publicado ahí para ejercer los derechos ARCO.

## 8. Glosario

| Término | Significado |
| :-- | :-- |
| OT | Orden de trabajo: lo que el mecánico ejecuta sobre el vehículo |
| DNI / RUC | Documento de persona (8 dígitos) / de empresa (11 dígitos) |
| Boleta | Comprobante a consumidor final; sobre S/ 700 exige DNI |
| Factura | Comprobante a empresa con RUC; da derecho a crédito fiscal |
| Serie y correlativo | Folio único del comprobante: serie `B001`/`F001` más número secuencial |
| IGV | Impuesto General a las Ventas, 18 %, incluido en los precios |
| Kardex | Cuenta corriente del producto: saldo inicial + entradas − salidas |
| Merma | Pérdida de inventario registrada con motivo |
| Utilidad estimada | Ingresos sin IGV − costo de lo consumido − gastos operativos |
