<div align="center">

# 📒 Changelog — AutoGestion

**Historial de cambios del sistema, en lenguaje humano.**
<br>
Lo nuevo arriba, lo viejo abajo. Sin humo: lo pendiente se marca ⏳.

<br>

<img src="https://img.shields.io/badge/version-2.18.0-orange?style=for-the-badge" />
<img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
<img src="https://img.shields.io/badge/Spring_Boot-3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />

</div>

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## 🗂️ Versiones

| Version | Fecha | Lo destacado |
| :-- | :-- | :-- |
| [2.18.0](#-2180---2026-10-29) | 2026-10-29 | 🛡️ Un solo camino de pago/entrega · una cotización vigente por diagnóstico (V10) · anular suelta el pago · refactor de cotización, rangos y Excel |
| [2.17.0](#-2170---2026-10-29) | 2026-10-29 | 🧾 Boleta y factura en A4 con el formato de representación impresa: recuadro del emisor, tabla por columnas, importes y texto legal |
| [2.16.0](#-2160---2026-10-29) | 2026-10-29 | 📊 Excel y PDF con portada, tablas paginadas y totales · CSV legible en Excel · Descarga sin bloqueos · Fix `no Session` en órdenes y aprobación de cotización |
| [2.15.0](#-2150---2026-10-29) | 2026-10-29 | 🎨 Rediseño visual de los paneles: superficies planas, un solo acento, tablas y formularios más legibles, modo claro y oscuro |
| [2.14.2](#-2142---2026-10-29) | 2026-10-29 | 🛡️ Auditoría final: stock y pagos serializados con bloqueo de fila · doble clic ya no duplica registros · base real sin descuadres |
| [2.14.1](#-2141---2026-10-29) | 2026-10-29 | 📚 Documentación de `docs/` reescrita (guía, manual, permisos, políticas) · Notas de defensa retiradas · Caja autorizada a recepción, como ya mostraba el menú |
| [2.14.0](#-2140---2026-10-29) | 2026-10-29 | 🧪 Avance 3: Guava y Commons Lang3 en el límite de consultas · Logback con archivo rotativo · Javadoc configurado · Migraciones compatibles con H2 · 98/98 pruebas en verde |
| [2.13.0](#-2130---2026-10-28) | 2026-10-28 | 🆓 DNI y RUC sin token: dniperu.com y openruc.com consultados desde el backend · CORS para `file://` y cualquier puerto local · Fin del “La fuente de consulta no respondió” por bloqueo del navegador |
| [2.12.0](#-2120---2026-10-28) | 2026-10-28 | 🧭 Login y panel del cliente rediseñados · Guía de uso y soporte · Fix real del autocompletado de DNI (sin datos de ejemplo) · Cotizaciones y solicitudes en el portal · Límite anti-enumeración · `fuente_verificacion` |
| [2.11.0](#-2110---2026-10-26) | 2026-10-26 | 🪟 Cliente primero: `/` = portal del cliente, `/admin/` = acceso del personal · Token real de cliente (rol `CLIENTE`) · Nginx reenvía `/api` (sin CORS) · Límite de intentos en identificación/OTP · Fix: la placa debe pertenecer al cliente · OTP seguro |
| [2.10.0](#-210---2026-10-23) | 2026-10-23 | 🚀 Portal cliente (seguridad + identificación modo A/B + OTP) · Nuevas ventanas (agenda, garantías, caja, checklist, compras, auditoría, cuentas, notificaciones, productividad, TV) · Políticas y cumplimiento (borradores) · Rediseño Bl4z3 base · Java 21 · Migración V5 |
| [2.9.0](#-290---2026-10-22) | 2026-10-22 | 🔧 Módulo Reportes refactorizado: 13 bugs críticos corregidos, PDFBox, Excel con freeze/autofilter, rango RangoFechas, seguridad por rol, XSS escapado, mecanismo por id |
| [2.8.0](#-280---2026-10-03) | 2026-10-03 | 🙋 Género del cliente + placas y persona de escala en el visor 3D |
| [2.7.0](#-270---2026-10-03) | 2026-10-03 | 🏎️ Renders de vehículos rediseñados (alta fidelidad) + visor 3D interactivo |
| [2.6.0](#-260---2026-10-02) | 2026-10-02 | 🚗 Preview de vehículos: marcas con logo, render y modelo manual |
| [2.5.0](#-250---2026-10-02) | 2026-10-02 | ✨ Jornada visual + SOLID + fixes de comprobantes (todo el día en una entrada) |
| [1.4.2](#-142---2026-10-01) | 2026-10-01 | 📄 Descarga PDF real + impresion sin encabezados del navegador |
| [1.4.1](#-141---2026-10-01) | 2026-10-01 | 🎫 Impresion fiel a pantalla + descarga de constancia |
| [1.4.0](#-140---2026-10-01) | 2026-10-01 | ✨ Rediseño visual guiado · wizard de pago · ticket pro · responsive real |
| [1.3.1](#-131---2026-10-01) | 2026-10-01 | 🐛 Fix crash boleta/factura · paneles exclusivos por rol |
| [1.3.0](#-130---2026-10-01) | 2026-10-01 | 🧾 Boleta/Factura al pagar + constancia imprimible · 📁 Reorden a la raiz |
| [1.2.0](#-120---2026-09-20) | 2026-09-20 | 🛡️ Permisos por rol + pagina 403 + stepper propio |
| [1.1.0](#-110---2026-09-20) | 2026-09-20 | ✨ Rediseño visual completo + tutorial + tour guiado |
| [1.0.0](#-100---2026-09-14) | 2026-09-14 | 🐛 Correccion de errores fundacionales (DTOs, pagos, cache) |

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## 🛡️ 2.18.0 — 2026-10-29

### Integridad de pagos, cotizaciones y comprobantes
- **Un solo camino de pago y entrega:** `PagoEntregaService` centraliza cobrar, entregar y vincular el pago al comprobante recién emitido. `POST /api/pagos` y `POST /api/entregas/{id}` se mantienen por compatibilidad, pero delegan en él; `OrdenTrabajoService` ya no guarda una copia propia. La entrega exige OT `FINALIZADA` (si no, `409` con mensaje claro) y fija su `fechaFin`, así que la orden y la recepción se cierran juntas.
- **Una sola cotización vigente por diagnóstico:** `CotizacionService` responde `409` si el diagnóstico ya tiene una cotización `PENDIENTE`, `EN_DIAGNOSTICO` o `APROBADA`. La migración `V10__cotizacion_vigente_unica.sql` lo respalda en la base con la columna `diagnostico_vigente_id` y un índice único (funciona igual en PostgreSQL y en H2, que no admite índices parciales). La migración repara duplicados heredados: conserva la `APROBADA` más reciente y rechaza el resto. Convertir a OT bloquea la fila, así que dos aprobaciones simultáneas no crean dos órdenes.
- **Anular ya no deja el pago apuntando al documento muerto:** `ComprobanteService.anular()` llama a `PagoEntregaService.desvincularComprobante()`; el pago conserva monto y fecha, pero deja de referenciar un comprobante sin validez.
- **Refactor de cotización:** `crear()` y `crearCompleta()` comparten un único flujo; se retiran unas 150 líneas duplicadas y las variables sin uso (`cli`, `veh`).
- **Un solo cálculo de rango:** `Rangos` resuelve fechas por defecto y valida `desde`/`hasta`; el controlador y la exportación usan esa misma regla, sin copias divergentes.
- **Excel con columnas declaradas:** cada columna indica su tipo (dinero, entero, promedio) y si suma total, en lugar de adivinar por nombre. `Horas prom.` ya no se suma dentro de una columna de dinero.
- **Limpieza:** `ResolutorAdquirente` sin ramas `if/else` vacías, import duplicado fuera de `ReporteService`, se elimina `frontend/js/reportes/export.js` (sin uso: `reportes.html` definía el suyo) y el Javadoc de `ComprobanteController.descargarPdf` dice A4, que es el tamaño real.
- **Verificación:** `mvn -B test` → **119 pruebas, 0 fallos** (19 nuevas: pagos/entregas, anulación, cotización vigente y rangos). README alineado con el código contado: 26 entidades, 13 enums, 25 repositorios, 25 servicios, 20 controladores, 48 DTOs y 10 migraciones.

## 🧾 2.17.0 — 2026-10-29

### Representación impresa del comprobante en A4
- **Formato de boleta/factura electrónica (`TicketPdf`):** el ticket de 80 mm pasa a una hoja A4 con la estructura habitual del comprobante: fecha de impresión y título `::: BOLETA DE VENTA ELECTRÓNICA - Impresión :::`, recuadro del emisor con su RUC y dirección, caja del número de serie, bloque de datos del adquirente (vencimiento, emisión, señor(es), documento, moneda, dirección y observación), tabla de detalle por columnas (`Cantidad`, `Unidad Medida`, `Descripción`, `Valor Unitario(*)`, `Descuento(*)`, `Importe de Venta(**)`, `ICBPER`), bloque de importes con `SON:` en letras, desglose de tributos en casillas y texto legal enmarcado con el pie de página.
- **Alineación medida, no estimada:** se incorporan las tablas de ancho de Helvetica y Helvetica-Bold, así que las columnas numéricas quedan alineadas al dígito y ningún texto se sale del marco. Las descripciones largas se reparten en dos líneas y, si no alcanzan, se recortan con puntos suspensivos.
- **Paginación:** el detalle se reparte solo en varias hojas cuando no cabe, con `Continúa en la hoja siguiente` y la numeración `1/2` en el pie.
- **Estado del comprobante:** `PAGO REGISTRADO` en verde o `COMPROBANTE ANULADO` en rojo con el motivo, ambos dentro del marco y en el pie.
- **Texto legal honesto:** el recuadro indica que es una representación de demostración del proyecto académico, que no fue firmada ni remitida a SUNAT y que carece de validez tributaria.
- **Vista previa igual al PDF (`pago_entrega.html`):** la constancia en pantalla y la impresión del navegador usan la misma estructura, con `@page A4`, márgenes de 10 mm y helpers propios de fecha, moneda y unidad de medida. El modal pasa a 760 px para leer el documento completo.
- **`SON:` sin duplicar:** el texto en letras de la base ya venía con el prefijo y se imprimía `SON: SON: ...`; ahora se detecta y se respeta.
- **Limpieza:** se retira el CSS `.ag-ticket` de `polish.css`, que quedó sin uso al cambiar el formato.
- **Verificación:** `mvn -B test` → **100 pruebas, 0 fallos** (2 nuevas: estructura del documento y paginación de 45 líneas). El PDF descargado desde el endpoint real mide A4 (595×842 pt), cabe en una hoja y no tiene texto fuera del marco; revisado también con la boleta, la factura y la anulada renderizadas.

## 📊 2.16.0 — 2026-10-29

### Exportaciones y flujo de órdenes
- **Excel rediseñado (`ReporteExcelGenerator`):** portada con banda de marca y contenido del reporte, una hoja por sección, título y subtítulo con rango y rol, encabezado ámbar, filas alternas, bordes finos, moneda con formato `S/ #,##0.00`, conteos enteros, totales con línea superior, filtro y paneles congelados, anchos calculados y área de impresión A4 horizontal. Los nombres de hoja se limpian y se recortan a 31 caracteres.
- **PDF rediseñado (`ReportePdfGenerator`):** A4 horizontal con portada de marca, tabla con columnas proporcionadas, ajuste de texto por celda, filas alternas, repetición de encabezado al pasar de página, notas al pie de cada sección y numeración `Página X de Y`. El texto se normaliza a WinAnsi para que acentos y símbolos no rompan la generación.
- **CSV legible:** el export del backend incluye BOM UTF-8 (Excel ya no muestra `DÃ­a`); los CSV de las tablas salen con encabezados en lenguaje natural, números sin comillas y decimal con coma, y solo se entrecomilla lo que contiene `;`, comillas o saltos de línea.
- **Descarga sin bloqueo:** el modal de exportación deshabilita el botón mientras genera, muestra `Generando...`, cierra siempre al terminar (también si hay error) y la petición tiene un límite de 90 s con mensaje claro. El enlace de descarga se elimina solo después de disparar el archivo.
- **Fix `LazyInitializationException` en órdenes:** el controlador respondía con la entidad fuera de la transacción y Hibernate ya no podía leer `Vehiculo`/`Cliente` (`could not initialize proxy - no Session`). Ahora responde con el DTO que construye el servicio dentro de su transacción, tanto al crear la OT como al cambiar de estado.
- **Flujo de cotización:** al aprobar, el wizard marca los tres pasos con ✓, deja de mostrar “falta el diagnóstico” y el aviso cambia a “Cotización aprobada — continúa en Órdenes de Trabajo”.
- **Verificación:** `mvn -B test` → **98 pruebas, 0 fallos**. Flujo completo probado contra PostgreSQL y la interfaz: recepción → diagnóstico → cotización → aprobación → OT → cambio a EN_PROCESO, sin `no Session`; los registros de prueba se eliminaron y la base demo quedó intacta.

## 🎨 2.15.0 — 2026-10-29

### Rediseño visual de los paneles
- **Sistema unificado (`css/refresh.css`):** superficies planas, un solo acento ámbar, bordes finos, radios de 12 px y jerarquía tipográfica más limpia. Se retiran las barras de gradiente, los brillos y las animaciones de entrada que hacían ruidosa la interfaz.
- **Dashboard:** los KPI usan chips de icono neutros con color solo cuando aporta significado; las tarjetas dejan de competir con sombras y bordes de colores. La guía superior queda compacta y el flujo del taller se mantiene legible.
- **Tablas y formularios:** cabeceras fijas, filas legibles al instante (se corrigió la opacidad que dejaban las animaciones al desactivarlas), inputs y botones de 8 px, y badges de tipo de producto neutros.
- **Modo claro y oscuro:** ambos usan los mismos tokens; el marco (barra superior y menú) se mantiene oscuro para conservar el contraste y la identidad del taller.
- **Alcance:** solo CSS y el enlace en las 13 páginas del panel. No cambian flujos, permisos, endpoints ni datos.
- **Verificación:** las 13 páginas responden 200, sin elementos invisibles ni desbordes en el flujo de pago; capturas en `docs/evidencia/rediseno-dashboard.png` y `docs/evidencia/rediseno-dashboard-claro.png`.

## 🛡️ 2.14.2 — 2026-10-29

### Integridad de datos bajo concurrencia
- **Stock con bloqueo de fila:** `ProductoRepository.bloquearPorId` (`PESSIMISTIC_WRITE`) y `InventarioService` lo usa en cada movimiento. Antes, dos consumos o ajustes simultáneos leían el mismo stock y el último guardado pisaba al otro: quedaba descuadrado y podía bajar de cero. Ahora las operaciones sobre el mismo producto se serializan.
- **Pago y entrega serializados:** `PagoEntregaService` y `OrdenTrabajoService.registrarPagoEntregaCompleto` bloquean la OT antes de escribir. Dos cobros o dos entregas simultáneas de la misma orden ya no compiten: la segunda recibe un conflicto claro.
- **Doble clic en el panel:** `apiFetch` comparte la misma promesa para POST idénticos en vuelo. Un doble clic o un reintento del navegador ya no crea dos diagnósticos, dos cotizaciones ni dos movimientos de inventario (afectaba a mermas, compras y consumos).
- **Sesión corrupta:** `getUser` tolera un JSON inválido en `localStorage` y devuelve al login en vez de romper la página completa.
- **Comentarios fuera del código:** se retiraron los comentarios `//` y `/* ... */` de Java, JavaScript, HTML y CSS (incluidos los scripts en línea). Se conserva el Javadoc (`/** ... */`) porque es el anexo de documentación técnica que evalúa la rúbrica. Las migraciones Flyway no se tocaron: cambiar su contenido alteraría el checksum y rompería el arranque sobre bases existentes.
- **Verificación:** `mvn -B test` → **98 pruebas, 0 fallos**. Auditoría SQL sobre la base PostgreSQL real: 0 productos con stock negativo, 0 comprobantes emitidos duplicados por OT, 0 totales descuadrados, 0 cotizaciones activas duplicadas y 0 clientes con documento repetido. El volumen `pgdata` conserva los datos entre reinicios.

## 📚 2.14.1 — 2026-10-29

### Documentación reescrita y permisos alineados
- **`docs/` vuelve a ser documentación de verdad:** la guía de desarrollo y el manual de usuario se habían quedado reducidos a fragmentos sin título (6 y 10 líneas). Ahora son documentos completos: arquitectura por capas, diagramas de flujo y del modelo de datos, reglas del proyecto, cómo agregar un reporte paso a paso, y manual por rol con el módulo de reportes, el portal del cliente y glosario. Se agrega `docs/README.md` como índice de la carpeta.
- **Matriz de permisos contra el código:** `docs/permisos.md` se reescribió a partir de las anotaciones `@RolesAllowed` reales, con el detalle por pestaña de reportes y el acceso del portal del cliente.
- **Fuera las notas de defensa:** se elimina `docs/PREGUNTAS_PROFE.md`, un listado de respuestas sueltas que no formaba parte del entregable.
- **Caja coherente con el menú:** `PagoEntregaController` estaba restringido a `ADMIN` a nivel de clase, pero el frontend ya mostraba *Pago y Entrega* a recepción y el manual describe que recepción cobra. El rol pasa a `{ADMIN, RECEPCIONISTA}`; el resto de endpoints no cambia.
- **`docs/evidencia/` protegido en `.gitignore`:** reglas de negación explícitas para que ningún patrón futuro excluya las capturas, los diagramas y los registros de prueba del informe.

## 🧪 2.14.0 — 2026-10-29

### Librerías de apoyo, registro de logs y documentación
- **Google Guava en el límite de consultas (`ConsultaDocumentoRateLimit`):** el `ConcurrentHashMap` con limpieza manual pasa a un `Cache` de Guava con `maximumSize(10_000)` y `expireAfterAccess(ventana)`: las IPs inactivas se expulsan solas, sin recorrer el mapa entero, y la memoria no crece sin control. `Preconditions.checkArgument` rechaza al arrancar una ventana mayor a 24 horas. Sigue siendo por instancia; para varias réplicas el contador debe ir a Redis o a Nginx.
- **Apache Commons Lang3 en la misma clase:** `StringUtils.defaultIfBlank(ip, "desconocida")` agrupa IP nula y en blanco en una sola clave (antes `isBlank` dejaba pasar cadenas de espacios con otra identidad).
- **Logback con archivo rotativo (`logback-spring.xml`):** mantiene la consola y añade `logs/autogestion.log` con rotación por tamaño y fecha — 10 MB por archivo, 14 días, 200 MB en total, comprimido en `.gz`. Los paquetes de Hibernate bajan a `WARN` para que el SQL no inunde el archivo. `logs/` quedó en `.gitignore`. El registro no incluye contraseñas, tokens ni documentos completos: los proveedores ya los enmascaran.
- **Javadoc configurado (`maven-javadoc-plugin` 3.10.1):** `mvn javadoc:javadoc` genera el anexo de documentación técnica en `backend/target/site/apidocs/` con `doclint=none` y UTF-8, para que los acentos del código no rompan la generación.
- **Pruebas del límite (TDD):** `ConsultaDocumentoRateLimitTest` — permite N consultas por ventana y bloquea la siguiente, cada IP lleva su propio contador, y `null` o espacios caen en la misma clave.
- **Migraciones que también corren en H2:** V5 deja de crear `otp_codigo` (el acceso por código se retiró) y quita el `DEFAULT gen_random_uuid()` de `solicitud_arco` (lo genera la entidad); V9 cambia el índice único parcial por uno normal sobre `(vehiculo_id, estado)` y el duplicado se valida en `SolicitudCotizacionService`. Motivo: las pruebas de integración corren sobre H2, donde ni los índices parciales ni esa función existen. En PostgreSQL el comportamiento es el mismo.
- **README alineado con el código:** tabla nueva con las cuatro librerías (versión, clase donde se usan y para qué sirve cada una) y un Paso 5 que explica cómo generar el JAR del backend con Docker, dónde queda y cómo ejecutarlo.
- **Limpieza:** se eliminan la entidad `SolicitudArco` y su repositorio, sin uso desde que se retiró el flujo de eliminación de cuenta; nada los inyectaba salvo una aserción del test de contexto, que también se limpió. La tabla `solicitud_arco` se queda en la migración V5 porque ya está aplicada (borrarla cambiaría el checksum de Flyway).
- **Verificación:** `mvn -B test` sobre Java 21 → **98 pruebas, 0 fallos**; `docker compose up --build` levanta el stack con las 9 migraciones validadas y el portal respondiendo en http://localhost:5501.

## 🆓 2.13.0 — 2026-10-28

### Autocompletado real de DNI y RUC sin tokens
- **`DniPeruProvider` (DNI, habilitado por defecto):** replica el flujo de `example DNI and RUC search` en el backend: calienta la cookie de sesión, pide el token antirrobot (`cc_get_tokens`) si el sitio lo exige y reintenta con `cc_token`/`cc_sig`; parsea el mensaje `Clave: valor` (acentos y CRLF incluidos) y arma apellidos + nombres. `DNIPERU_DNI_HABILITADO=false` lo apaga.
- **`OpenRucProvider` (RUC, habilitado por defecto):** consulta `openruc.com/api/ruc/{ruc}` sin clave y devuelve razón social, estado, condición y dirección. `{"error":"not_found"}`/404 = no encontrado; 5xx = error técnico para seguir con otra fuente.
- **Causa del mensaje “La fuente de consulta no respondió”:** el navegador intentaba llamar a esas fuentes (o al backend desde `file://`) y el navegador bloqueaba la petición por CORS; el `catch` del portal lo mostraba como `ERROR`. Ahora la consulta externa la hace el servidor y `SecurityConfig` acepta `Origin: null` (doble clic sobre `index.html`) y cualquier puerto de `localhost`/`127.0.0.1` en desarrollo.
- **Seguridad:** el token del portal sigue viajando en la cabecera `Authorization` (no en cookies), el límite de 12 consultas/10 min por IP se mantiene y los logs solo muestran el documento enmascarado. Las fuentes nuevas son terceros no oficiales: sus términos restringen la consulta automatizada, por eso quedan detrás de una variable de entorno.
- **Pruebas:** `DniPeruProviderTest` (7) y `OpenRucProviderTest` (6) contra un `HttpServer` local: flujo con token, sin token, respuesta sin nombres, DNI/RUC inválidos, fuente apagada, 404 y 5xx. Además se probaron las dos fuentes reales desde el código compilado (DNI 70716281 → `VALLE COBEÑAS CARLOS JAVIER`; RUC 20131312955 → SUNAT ACTIVO/HABIDO) y `CorsConfiguration.checkOrigin` con `null`, `localhost:8000` y un origen externo rechazado.

## 🧭 2.12.0 — 2026-10-28

### Portal del cliente (login, panel y soporte)
- **Login/registro rediseñado** (`frontend/index.html`): layout de marca + formulario, pestañas accesibles (Documento + placa / Código / Crear cuenta), validaciones de longitud y formato, estados de carga y errores específicos (401, 429, red). El registro tiene 3 pasos y conserva el modo manual cuando no hay fuente de nombres.
- **Panel del cliente** (`portal/estado.html`): saludo con identidad, documento enmascarado, etiqueta de origen de los datos, resumen real (órdenes activas, vehículos, total), última orden, flujo de estados, vehículos, perfil con edición de celular/correo (`PATCH /api/portal/me/contacto`), acciones rápidas, guía/soporte, skeletons, estados vacíos, reintento y menú de cuenta con cierre de sesión confirmado.
- **Sistema visual único** (`css/portal-ui.css` + `js/portal-ui.js`): acero + ámbar, foco visible, cabecera compartida, navegación desplazable en móvil, diálogos adaptables y sin scroll horizontal.
- **Guía de uso** (`portal/ayuda.html`): 9 pasos reales, 8 preguntas frecuentes, solución de problemas y advertencias de privacidad.
- **Soporte** (`portal/soporte.html`): solo muestra canales configurados (`APP_EMPRESA_EMAIL`, `APP_EMPRESA_WHATSAPP`, `APP_EMPRESA_TELEFONO`, `APP_EMPRESA_DIRECCION`); el reporte se prepara para el correo y avisa que la página no lo envía. Si falta un canal, se indica sin inventarlo.
- **Fix de codificación:** los valores por defecto de `application*.properties` con acentos usan escapes Unicode (`Mart\u00EDn`), evitando “MartÃ­n” en la API.
- **SUNAT sin API key:** nuevo `SunatDniProvider` que convierte el DNI en su RUC 10 y busca la razón social en el Padrón Reducido RUC local. Se importaron 18.491.506 filas del archivo oficial (2026-10-28). Cubre solo personas con RUC 10 y no es verificación de identidad.
- **Scrall para DNI sin RUC 10:** nuevo `ScrallDniProvider` con `Authorization: Basic`, POST JSON y parseo de nombres/apellidos. Requiere `SCRALL_API_KEY` del plan API Básico gratuito; sin clave queda deshabilitado. No se usa el endpoint sin autenticación.
- **Cotizaciones del portal:** `GET /api/portal/cotizaciones`, `GET /api/portal/cotizaciones/{id}`, `POST .../aprobar` y `POST .../rechazar`. El cliente solo ve sus propias cotizaciones (consulta por `clienteId` del token; un id ajeno responde 404) y la página dejó de ser texto: muestra detalle, total, servicios/repuestos y confirmación antes de decidir. La creación del presupuesto sigue siendo del taller desde recepción → diagnóstico; el cliente no inventa precios.
- **Solicitudes de cotización desde el panel:** `GET/POST /api/portal/solicitudes-cotizacion` y bandeja del personal `GET /api/solicitudes-cotizacion?estado=`, `PUT .../atender`, `PUT .../rechazar`. El cliente elige su vehículo y describe el problema; se valida que la placa sea suya, se impide duplicar una solicitud pendiente por placa y se limita a 3 pendientes por cliente. El taller responde con una nota que el cliente ve en `cotizacion.html`. La tabla `solicitud_cotizacion` se crea con Flyway V9; no guarda precios.

### DNI: causa raíz y corrección
- **Causa raíz:** el único proveedor de DNI (`ReniecDniProvider`) apuntaba a un servicio que exige token; sin token se llamaba igual y el 401 se convertía en “sin datos”. El portal de registro no tenía autocompletado (solo un campo manual) y los formularios del personal solo validaban formato. No había forma de distinguir “sin fuente”, “no encontrado” y “error”.
- **Proveedor:** `disponible()` para no llamar fuentes sin credenciales; URL con `{numero}` o `?numero=`; parseo de camelCase/snake_case/full_name; 404/422 = no encontrado; fuente real en la respuesta.
- **Servicio:** motivos (`OK`, `SIN_FUENTE`, `NO_ENCONTRADO`, `ERROR`, `INVALIDO`, `LIMITE`), caché positiva 24 h / negativa 5 min / error 1 min, y registro de documento enmascarado.
- **Frontend:** consulta automática solo con 8/11 dígitos, `AbortController` + secuencia para descartar respuestas viejas, limpieza de nombres al cambiar el documento, respeto de nombres editados a mano y bloqueo de “continuar” sin datos.
- **Backend autoritativo:** con fuente disponible se ignora el nombre enviado por el navegador; se guarda `fuente_verificacion` (V7) y `documento_verificado`. V8 normaliza registros antiguos sin fuente a “datos declarados”.
- **Personal:** nuevo `GET /api/documentos/consultar` (ADMIN/RECEPCIONISTA/MECANICO); `clientes.html` y `recepcion.html` autocompletan solo si el campo está vacío o fue autocompletado.
- **Anti-abuso:** límite de 12 consultas/10 min por IP en el backend, además de Nginx; se usa `X-Real-IP` y el último `X-Forwarded-For` para evitar spoofing.

### Pruebas ejecutadas
- `mvn -Dtest=CotizacionPortalTest,ScrallDniProviderTest,SunatDniProviderTest,ConsultaDocumentoServiceTest,ClienteServiceTest test`: en verde (incluye DNI→RUC 10, Scrall con servidor HTTP local, listado de cotizaciones y rechazo de id ajeno).
- `mvn -Dtest=SolicitudCotizacionServiceTest test`: en verde (creación con vehículo propio, rechazo de placa ajena, bloqueo de duplicados y de una solicitud ya atendida).
- Stack real con PostgreSQL + Flyway V1→V8: login, dashboard, edición de contacto con validación, logout, registro manual, cambio de documento que limpia nombres, guía/soporte, autocompletado del personal y límite 429.
- Ruta de éxito con un mock local autorizado: el nombre falso enviado por el cliente fue ignorado y se guardó el nombre de la fuente con `fuente_verificacion`.
- **Suite completa:** 98/98 en verde después de corregir los fallos de entorno (índices parciales en V5/V9 y `DEFAULT gen_random_uuid()`, no soportados por H2, y dos aserciones de PDF/Excel con formatos antiguos).
- **Ruta real de SUNAT:** padrón oficial importado (18.491.506 filas, 2.4 GB); `GET /api/public/portal/documento/consultar?tipo=DNI&numero=...` devuelve `NO_ENCONTRADO` para un DNI sin RUC 10 y `OK` con `fuente=SUNAT · Padrón Reducido (RUC 10)` cuando existe. La prueba de éxito usó una fila sintética y se eliminó después.

### Pendiente ⏳
- El padrón SUNAT ya cubre a las personas con RUC 10. Para los DNI sin RUC 10 hace falta un proveedor autorizado y su token (`RENIEC_DNI_TOKEN`); no existe API pública gratuita de RENIEC. Sin coincidencia, el portal funciona en modo manual y lo dice.
- Comprobantes, historial, citas y encuesta siguen sin endpoint de portal: muestran estados honestos, sin formularios que no guardan nada. Cotización ya tiene listado, aprobación/rechazo y solicitud previa; el presupuesto con montos lo sigue generando el taller.

## 🪟 2.11.0 — 2026-10-26

### Orden de ventanas
- `/` ahora es el **portal del cliente** (identificación por documento + placa, o por código OTP).
- `/admin/` es el **acceso del personal** (login + paneles por rol). El login del personal dejó de estar en la raíz.
- Carpetas: `frontend/index.html` = portal, `frontend/portal/*` = páginas del cliente, `frontend/admin/index.html` = login del personal, `frontend/admin/pages/*` = paneles. `css/` y `js/` se comparten con rutas absolutas (`/css/…`, `/js/…`).
- Enlace discreto "Acceso personal" en el portal. `/admin/` va con `X-Robots-Tag: noindex`.

### Docker / Nginx
- Nginx reenvía `/api/` al backend: el navegador usa un solo origen (`http://localhost:5500`), sin CORS.
- `limit_req` en `/api/public/` (20 req/min por IP, ráfaga 10) para frenar fuerza bruta sobre identificación y OTP.
- Puertos de `backend` (8080) y `db` (5433) publicados solo en `127.0.0.1`.
- `api.js` usa `/api` relativo; con Live Server (5501/5502) o `file://` cae a `http://localhost:8080/api`.

### Seguridad (Java)
- **Fix:** el modo A validaba documento y placa por separado; ahora la placa tiene que ser del cliente.
- Token de sesión de cliente: JWT con `tipo=CLIENTE`, subject `c:<id>`, vence en 30 min (`app.portal.token-minutos`). El filtro le da solo `ROLE_CLIENTE`.
- `SecurityConfig`: `/api/portal/**` → solo `CLIENTE`; `/api/**` → solo roles del personal. Un token de cliente no abre endpoints del personal.
- `GET /api/portal/me`: el id del cliente sale del token, nunca del request.
- OTP: `SecureRandom`, hash con sal, el código en claro ya no se guarda y solo se borran los códigos de ese documento (antes `deleteAllInBatch` borraba los de todos). Máx. 3 intentos.
- Log de acceso con SHA-256 del documento y de la IP.

### Portal (JS)
- Nuevo `js/portal-api.js`: token en `sessionStorage` (se borra al cerrar la pestaña), separado del token del personal.
- **Fix:** el portal llamaba `/api/api/public/…` (doble prefijo) usando el `api.js` del personal.
- `portal/estado.html` conectado a `/api/portal/me`. Las demás páginas privadas del portal exigen sesión.

### Pendiente ⏳
- `portal/cotizacion`, `comprobantes`, `historial`, `mantenimiento`, `cita`, `encuesta` siguen estáticas (sin endpoint).
- `LogNotificador` imprime el OTP en el log: solo para desarrollo. En producción, notificador real (WhatsApp/SMS).

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## 🙋 2.8.0 — 2026-10-03

### Género del cliente + placa y persona de escala en el visor 3D

- **Género opcional del cliente:** nuevo campo `genero`
  (`MASCULINO` / `FEMENINO` / `OTROS`, o sin especificar) con enum propio
  (`Genero`), validación en `ClienteRequest`, migración
  `V4__cliente_genero.sql` (idempotente, con CHECK, PG16 + H2) y presencia
  en `GET /clientes/{id}/resumen`. Selector en el formulario de
  `clientes.html` y en el paso 1 del wizard de `recepcion.html` (alta con
  `clienteGenero`); la identidad del cliente sigue siendo (tipoDocumento,
  documento). Semilla demo con género asignado.
- **Placas delantera y trasera en 3D:** el visor dibuja ahora la placa real
  del vehículo (canvas con el texto de la placa, estilo de la app) en el
  parachoques frontal y en el trasero, calculando la posición sobre el
  contorno real de la carrocería. Si no hay placa muestra `___-___`.
- **Mini persona junto a la puerta:** figura humana de escala (altura ≈ 1.15
  × la del auto) junto a la puerta del conductor, cuyo diseño cambia con el
  género del cliente: masculino (hombros anchos, pelo corto, polo azul),
  femenino (hombros estrechos, pelo largo, top malva) y otros/sin
  especificar (complexión media, pelo medio, verde). El género viaja del
  formulario → `data-genero` del botón 3D → `vehAbrir3D(v)`.
- **Vista inicial:** el visor abre en frontal 3/4 del lado del conductor
  para que se vean la placa y la persona de entrada.
- **Verificado:** `mvn test` 49/49 en verde (2 tests nuevos de género:
  guardado/resumen y parseo) + `node --check` + navegador: 3 variantes de
  género montadas, placa delantera/trasera visibles y 12 aperturas/cierres
  seguidos sin fallos ni fugas.

## 🏎️ 2.7.0 — 2026-10-03

### Renders rediseñados con fidelidad real + visor 3D interactivo

- **`vehiculos.js` reescrito:** las 4 siluetas genéricas pasan a
  carrocerías parametrizadas por modelo real (sedán, hatch, SUV, pickup y
  van/vagoneta) con tabla de modelos (Hilux, Tucson, Sportage, HR-V,
  Picanto, Yaris, Urvan, Hiace…), proporciones propias por segmento,
  pintura metálica con degradados, faros y parrilla recortados a la
  silueta, llantas con pinza de freno, cintura lateral, líneas de puerta,
  espejos, manijas y sombra de contacto. Mismo API de siempre
  (`vehPreviewHTML`, `vehMiniHTML`, `vehPerfil`, guards intactos).
- **Nuevo visor 3D (`vehiculo3d.js` + three.js r160 local en
  `js/vendor/`, sin CDN):** botón "Ver en 3D" en previews y minis. La
  MISMA silueta 2D se extruye con taper de techo y morro, así el 3D
  replica las proporciones del render lateral exactas. Pintura PBR con
  clearcoat, cristales, faros divididos a los lados, parrilla, faro
  trasero continuo tipo Tucson, llantas con radios, entorno de estudio
  (PMREM), sombras suaves y sombra de contacto.
- **Controles:** arrastrar con inercia, rueda/pellizco para zoom,
  flechas/±, reset, auto-rotación tras 2.6 s (apagable; off en móvil),
  táctil completo y ESC para cerrar. Render bajo demanda con pausa por
  visibilidad/pestaña, DPR limitado (1.5 en móvil, 1.25 con ≤4 GB RAM).
- **Ciclo de vida sin fugas:** el contexto WebGL, la escena base y el
  entorno se reutilizan entre aperturas (crear un contexto por apertura
  colgaba los frames en aperturas rápidas); al cerrar se liberan
  geometrías, materiales y texturas del vehículo y se desconectan
  listeners, observers y rAF. Verificado con 12 aperturas/cierres
  seguidos: la escena no crece ni acumula contextos.
- **Compatibilidad:** sin WebGL o si falla el import de three, el botón
  3D no aparece o cae al render SVG con aviso (fallback). Integrado en
  `recepcion.html` y `clientes.html` (`?v=20261003`), responsive hasta
  390 px y `prefers-reduced-motion` respetado. Estilos en las secciones
  19/20 de `panels-premium.css`.

## 🚗 2.6.0 — 2026-10-02

### Preview de vehículos con marca, logo y render

- **Nuevo `js/vehiculos.js`:** catálogo de 12 marcas (las más vistas en
  talleres del Perú) con emblema propio, color y carrocería; 4 renders SVG
  laterales (sedán, SUV, pickup, hatch) con llantas, vidrios, luces, placa
  y emblema en la puerta. Marcas libres usan color estable por hash de placa.
- **Recepción, paso Vehículo:** la marca ahora es dropdown (Toyota, Hyundai,
  Kia, Nissan, Honda, Chevrolet, Volkswagen, Suzuki, Mazda, Ford, Mitsubishi
  + Otra marca con texto libre) y el modelo sigue manual; debajo hay un
  **preview en vivo** (render + logo + placa estilo Perú + modelo/año) que se
  arma mientras escribes. Sin cambios de backend: marca/modelo ya eran texto.
- **Donde más se ve:** logo en las tarjetas de vehículo del wizard, preview
  completo en el paso de confirmación y minis con render en la ficha del
  cliente. Todo con guards: si el script no carga, los formularios siguen
  funcionando.

## ✨ 2.5.0 — 2026-10-02

Jornada completa del 2 de octubre en una sola entrada: rediseño visual de los paneles, iconos duotono, responsive real, principios SOLID en el backend y fixes de comprobantes con PDF en Java.

### 🔓 2.4.2

#### CORS: Live Server también en el puerto alterno

- `SecurityConfig.corsConfigurationSource()` acepta ahora
  `http://localhost:5501` y `http://127.0.0.1:5501`: cuando el 5500 está
  ocupado, VS Code abre el frontend en el 5501 y el login dejaba de llegar
  al backend por bloqueo CORS. Sin cambios en métodos, headers ni
  credenciales (siguen igual).
- ⚠️ Requiere reiniciar el backend para tomar efecto.

### 🔦 2.4.1

#### El tour ahora ilumina solo lo importante

- Efecto *spotlight*: el recuadro ámbar proyecta una sombra gigante que
  atenúa toda la pantalla (fade semi oscuro) menos el hueco del elemento
  explicado; el anillo y el glow quedan en el borde para que se entienda
  qué se está señalando.
- Si un paso no tiene objetivo visible, el overlay atenúa todo y el popover
  queda centrado. El hueco sigue a su elemento si se hace scroll o se
  redimensiona (reposicionado con `requestAnimationFrame`).

### ✨ 2.4.0

#### Set de iconos duotono propio

- Adiós al Feather plano: trazo 1.8, remates redondos y capa suave
  (`fill` con opacidad) que da profundidad sin perder legibilidad a 15px.
- Metáforas nuevas por módulo: volante (Recepción), casco (Mecánico),
  headset (Recepcionista), ficha-cliente (Clientes), billete (Pago),
  cubo con cara superior (Inventario), libro (Tutorial), portapapeles
  con check, monedas y alerta redibujada para los KPIs.
- Aplicado en el sidebar de **todos** los paneles (`NAV_ICONS` en `api.js`),
  cabeceras de los 8 paneles con icono, tiles y KPIs del dashboard, mapa de
  iconos de reportes, guía contextual, FAB de ayuda y alerta de `api.js`.
  Sin cambios de estructura: solo el `path` de cada SVG.

### 🧹 2.3.0

#### Adiós al nombre personal: el admin es "Administrador" en todos lados

- Seed (`DataInitializer`): el admin de arranque nace como `Administrador`
  (sin apellidos) y `renombrarAdmin()` renombra instalaciones viejas en el
  arranque, sin tocar clave, rol ni documento. Los demás demo conservan su nombre.
- README: la tabla de credenciales dice `Administrador`.
- Ya no queda ninguna aparición del nombre anterior en código, seed ni docs
  (el CHANGELOG lo menciona solo como registro histórico de este cambio).

#### Responsive real: nada se corta y el móvil respira

- **Causa del corte lateral:** `.ag-page` llevaba `width: 100%` + `margin-left`
  del sidebar = más que el viewport; la columna derecha se salía. Ahora es
  `width: auto` y la página llena justo lo disponible, colapsado o no.
- **Reflow por tramos:** ≤1400px riel de 340px; ≤1250px formularios y layout
  ancho a una columna, KPIs 2×2, guía con wrap y kanban con scroll horizontal;
  ≤1100/768/480 los cortes previos (una columna, tiles 1×1).
- **Móvil (360px):** topbar compacta (salir solo-icono, pill y paddings
  reducidos), buscadores de cabecera a fila completa, tiles a una columna,
  tablas con scroll suave. Sin cambios de HTML: todo por CSS.

### ✨ 2.2.0

#### Pill sin nombre personal

- El pill del topbar ya no muestra "Carmen Rosa Huamán Díaz": solo el rol
  (Administrador, Mecánico, Recepcionista, Almacenero) con su punto de color.
  Vale para todos los roles, en claro y oscuro.

#### Layout fluido sin huecos (como el login)

- `.ag-page` sin `max-width`: la página ocupa todo el viewport menos el
  sidebar, y las columnas usan `minmax(0, 1fr)` para crecer/encoger. Al
  colapsar el menú o achicar la ventana ya no quedan franjas vacías.
- KPIs y layouts con `minmax` + mismos cortes responsive de antes
  (4 → 2 → 1 columnas); los hijos de grid llevan `min-width: 0` para que
  las tablas largas no rompan el ancho.

#### Dashboard estilo login

- Acceso Rápido convertido en *tiles* verticales (icono degradado, título,
  descripción y flecha) como las `.lg-tile` del login; la grilla es
  `auto-fill/minmax`, así se reacomoda sola al redimensionar.
- Flujo del Taller como *chips* (eco de `lg-flow`): pildoras que se envuelven
  (antes se cortaba en pantallas angostas) y el paso inicial nace activo.

#### SOLID en el backend (sin cambiar comportamiento ni API)

- **SRP:** `ComprobanteService` (444 líneas, 6 responsabilidades) ahora solo
  orquesta. Nace `service/comprobante/` con `ResolutorAdquirente` (reglas del
  adquirente), `ConstructorDetalle` (líneas, IGV y totales), `ComprobanteMapper`
  (DTO puro y sin estado) y `OrdenSeguro` (whitelist del ORDER BY nativo).
- **SRP/DRY:** `OrdenTrabajoService.crear/crearCompleta` comparten
  `abrirOrden()` + validadores (`cotizacionAprobada`, `mecanico`).
- **OCP:** `EstadoOT.puedePasarA()` y `estaCerrada()` mudan la máquina de
  estados al enum (switch exhaustivo: un estado nuevo no compila si no se
  cubre). `DocumentoValidator` pasa a Strategy: `ReglaDocumento` + 4 reglas
  (`util/documento/`), fachada con el mismo API para no tocar llamadas ni tests.
- **DIP/ISP/LSP:** los nuevos componentes dependen de repositorios
  (abstracciones) y de `EmpresaProperties`; interfaces pequeñas y cohesivas;
  los enums se comportan igual en todos sus usos.
- ⚠️ No se pudo compilar aquí (sin Maven en esta máquina): el refactor es
  extracción verbatim con firmas intactas y quedó verificado por revisión
  (sin referencias huérfanas, llaves balanceadas, tests existentes compatibles).
  Corre `mvn test` antes de subir.

### ✨ 2.1.0

Los paneles internos heredan el rediseño del login, sin tocar lógica ni
estructura HTML. Todo vive en una capa nueva: `frontend/css/panels-premium.css`.

- **Mismo idioma visual:** iconos ámbar con glow (topbar, sidebar, cabeceras,
  guía, KPIs), tarjetas con barra degradada superior y titulares Space Grotesk.
- **Entrada escalonada:** cabeceras, guía, KPIs y secciones aparecen con
  `fade-up` en cascada, como la tarjeta del login.
- **Detalles premium:** cabecera fija en tablas con guía ámbar al pasar el
  mouse, montos de estado con texto degradado, sidebar con retícula y grupos
  estilo *eyebrow*, insignia "Tiempo real" en el dashboard.
- **Alcance:** los 13 HTML de `pages/` enlazan la hoja; el dashboard además
  estrena cabecera `.ag-head` con icono y meta. Claro/oscuro y responsive
  incluidos, con respeto a `prefers-reduced-motion`.

### 🟡 2.0.1

Dos errores que solo saltaban con la app corriendo, más limpieza de archivo y
la migración del generador de PDF de JavaScript a Java.

#### 🐛 El historial de comprobantes devolvía error 500 (SQL nativo + Sort)

- **Causa:** `GET /comprobantes?sort=fechaEmision,desc` llegaba a una consulta
  NATIVA y Spring Data mete el `Sort` en el `ORDER BY` sin traducir nombres de
  propiedad, así que PostgreSQL recibía `ORDER BY c.fechaEmision` y respondía
  *column c.fechaemision does not exist* (la columna es `c.fecha_emision`).
- **Fix:** `ComprobanteService` traduce el sort a columnas reales con una
  lista blanca (`fechaEmision` → `fecha_emision`, `id`, `numero`, `total`) y
  fija un orden por defecto determinista (`fecha_emision DESC, id DESC`) para
  que la paginación no revuelva filas. De paso, la lista blanca cierra la
  inyección vía `?sort=`.
- **Test:** `ComprobanteListadoTest` pide exactamente el sort del front y
  fallaría con el error viejo.

#### 🐛 "El DNI debe tener 8 dígitos (llevas 8)" al emitir la boleta

- **Causa:** el front mandaba `tipoDoc: f.tipo` (el tipo del COMPROBANTE:
  `BOLETA`), que no es un tipo de documento; el backend lo volvía `null` y
  rechazaba con un mensaje calculado para DNI que se contradecía con el dato.
- **Fix en el front:** `pago_entrega.html` manda `RUC` para factura,
  `SIN_DOC` si no hay documento y `DNI` para boleta.
- **Fix en el backend (el que vale aunque el front esté cacheado):**
  `resolverAdquirente` deduce el tipo del número y de la ficha del cliente
  cuando el declarado no corresponde, y solo rechaza si el número sigue sin
  servir. Un RUC en boleta sigue diciendo "corresponde factura".
- **Mensajes honestos:** `DocumentoValidator` ya no dice "8 dígitos (llevas
  8)" cuando hay letras: explica que son 8 caracteres pero no todos dígitos.

#### 📄 El PDF del ticket se migra de JavaScript a Java

- **Nuevo `util/TicketPdf`:** ticket de 80 mm en PDF puro (sin librerías),
  mismo diseño que el generador que vivía en el HTML: cabecera, folio,
  adquirente, detalle, IGV, total en letras, QR y leyendas. Codificado en
  WinAnsi con paréntesis escapados y offsets de `xref` calculados en bytes.
- **Nuevo endpoint:** `GET /api/comprobantes/{id}/pdf` devuelve el archivo con
  `Content-Disposition` (roles ADMIN/RECEPCIONISTA como el resto).
- **Front:** `descargarComprobante()` ahora baja el blob del backend vía el
  nuevo `apiFetchBlob()` y se borran ~100 líneas de dibujo de PDF del HTML.
- **Tests:** `TicketPdfTest` valida estructura (cabecera, xref coherente,
  cierre), datos fiscales, escape de paréntesis y leyenda de anulado.

#### 🧹 Refactor y limpieza

- **`util/Rangos`:** un solo lugar para el rango por defecto de los listados
  («desde 2000-01-01», «hasta exclusivo = día siguiente») y para la zona
  America/Lima: `Comprobante`, `Inventario` y `Gasto` dejan de reimplementarlo
  con `LocalDate.now()` del servidor.
- **`apiFetch` en el front:** la traducción de errores HTTP (401, 403,
  `BusinessException`, validación Bean) vive solo en `agFalla()`, que usan
  `apiFetch` y `apiFetchBlob`.
- **Historial con `URLSearchParams`:** el campo de búsqueda `q` se mandaba
  al backend (antes había código muerto que intentaba limpiar un `q` que nunca
  se incluía en la URL).
- **Borrados:** `sistemaautogestion.zip` (copia obsoleta del repo con `.git`),
  `frontend/js/icons.js` (nadie lo cargaba) y `database/migrations/*.sql`
  (reemplazados por Flyway V1/V2; el Dockerfile ya no los copiaba). Árbol del
  README actualizado (30 entidades, 18 repos, 14 servicios, 17 controladores,
  37 DTOs, `util/` y `db/migration/`).

#### 🗄️ Migración V3: el CHECK de `cotizacion.estado` en H2

- **Bug latente:** V1 creó el CHECK inline sin nombre. PostgreSQL lo bautiza
  `cotizacion_estado_check` (por eso V2 lo arregló allí), pero H2 le pone
  `CONSTRAINT_xxx` y el CHECK viejo seguía vivo: en H2/dev, crear una OT
  (cotización → `CONVERTIDA`) revienta con *Check constraint violation*.
- **`V3__cotizacion_estado_check_unificado.sql`:** recrea la columna con un
  único CHECK con nombre estable y redeclara el índice. V1/V2 no se tocan
  (cambiarlos rompería el checksum de instalaciones ya migradas).
- **Los IT por fin corren:** surefire solo ejecutaba `*Test`, así que
  `ReporteFlujoIT` nunca se había corrido; el `pom` ahora incluye `*IT.java`.
- **Verificado:** `mvn test` **47/47 en verde** (11 clases, incluye
  `ReporteFlujoIT` y `ComprobanteListadoTest`) + `node --check` sobre `api.js`
  y los scripts inline de `pago_entrega.html`.

#### 🔎 Segunda tanda: búsqueda del equipo rota en PostgreSQL + export de reportes

- **`GET /api/usuarios?...&q=` devolvía 400 con PostgreSQL:** Hibernate no sabe
  tipar un parámetro dentro de `CONCAT('%', :q, '%')` y lo ata como `bytea`,
  así que `LOWER(...)` revienta con *function lower(bytea) does not exist*.
  Solo pasa con la búsqueda vacía (el servicio manda `null`): por eso el equipo
  fallaba al abrir y clientes no. Arreglado en `UsuarioRepository` y
  `ClienteRepository`: el patrón lo arma `util/Like.patron(q)` y se compara
  directo (`LOWER(col) LIKE :q`), sin CONCAT. Además la búsqueda por documento
  y teléfono ahora también ignora mayúsculas.
- **Verificado contra PostgreSQL 16 real** (contenedor efímero + Flyway
  V1/V2/V3 + backend arrancado): `/api/usuarios` (con y sin `q`),
  `/api/clientes/buscar`, `/api/comprobantes?sort=fechaEmision,desc` y el CSV
  de reportes → **200**. Sin token, el CSV → 401 (lo que hacía el enlace viejo).
- **Export «Clientes por día» roto:** era un `<a href="/api/reportes/...">`:
  en el puerto del frontend daba 404 y ni siquiera llevaba el JWT. Ahora es un
  botón que baja el archivo con el nuevo `apiDescargar()` de `api.js` (mismo
  camino que la descarga del PDF).
- **Auditoría de las 14 páginas HTML:** 0 errores de sintaxis, 0 llamadas a
  funciones inexistentes, 0 ids referenciados que falten, 0 recursos colgados
  y 0 endpoints del front sin ruta en el backend. El único bug real era el
  enlace de export de reportes.

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## 🟡 2.0.0 — 2026-10-02

<img src="./readme-assets/ver-200.svg" width="380" alt="v2.0.0 fases 1 a 9 en una sola versión" />

Todo lo del 2026-10-02 salió junto, así que va en una sola versión con una sección por fase.

### 🧭 Navegación lateral + fixes de RUC y OT (misma 2.0.0)

- **Sidebar hamburguesa:** el menú superior se vuelve barra fina (hamburguesa + marca + usuario) y todo vive en un panel lateral agrupado por flujo (Operación, Taller, Caja, Almacén, Gestión, Ayuda) con solo tus páginas por rol. En PC se fija o colapsa a iconos (recuerda tu elección); en móvil/tablet es cajón deslizable con overlay y Esc. Impresión limpia sin menús.
- **RUC que sí ayuda:** el validador ahora dice el dígito esperado ("debería terminar en 4") y sugiere el comprobante correcto según el largo (11 dígitos en boleta → vete a FACTURA; 8 en factura → vete a BOLETA). Ojo honesto: `20707162819` no pasa SUNAT (dv real 4, termina en 9): se rechaza a propósito, con test que lo fija.
- **OT con mecánico pre-asignado:** al elegir cotización se preselecciona el mecánico de su diagnóstico (con aviso); si no, el último que usaste. `GET /cotizaciones` ahora trae `mecanicoId`/`mecanicoNombre`.
- Verificado: `mvn test` 37/37 + sintaxis JS de las 3 páginas tocadas.

### 🗄️ Flyway en Docker: módulo PG + fuente única (misma 2.0.0)

- **Causa del bucle:** Flyway 10 separó el soporte por BD en módulos; sin `flyway-database-postgresql` el backend moría con `Unsupported Database: PostgreSQL 16` antes de migrar. Agregado al `pom` (versión gestionada por Boot, 10.10.0, sin versión puesta a mano).
- **Fuente única:** `db/migration/` con `V1__esquema_inicial.sql` (14 tablas originales) + `V2__fiscal_comprobantes_equipo_taller.sql` (todo lo demás, idempotente). Se borraron `db/migration/postgresql|h2` y `database/init.sql`; el Dockerfile de Postgres ya no copia nada al entrypoint.
- **Mismo SQL en PG16 y H2-modo-PG:** `BIGSERIAL`/`TEXT`/`ON CONFLICT` valen en ambos; lo que no (`ALTER` multi-columna con comas, índice parcial, FK agregada después) se escribe separado o lo valida el servicio. Verificado: `mvn test` 36/36 + boot real contra PG 16.15 con `validate` verde + login JWT OK.
- **Casos probados:** volumen viejo (esquema original + datos, `baseline` → V2, `AJUSTE`→`AJUSTE_POSITIVO`, re-ejecución = `up to date`) y volumen limpio (V1+V2, 20 tablas, seed + login).

### 🔒 Fase 9: producción sin secretos en el repo

- **Secretos a entorno:** `.env` (con `.env.example`, ignorado por git) gobierna DB, JWT, admin y claves demo. En `application-docker.properties` no queda ningún valor real. Consola H2 y `frameOptions` abierto solo en desarrollo.
- **Seed por bandera:** el ADMIN de arranque y las series siempre; la demo (equipo, 5 clientes con 2 RUC válidos, vehículos, catálogos) solo con `APP_SEED_DEMO=true`. Claves demo distintas por rol.
- **Estados tipados:** `EstadoOT`, `EstadoRecepcion`, `EstadoCotizacion` en entidades (mismo texto en BD, cero migración); transiciones inválidas responden 400/409 con campo y sugerencia. El `PROGRESS` del frontend ya usaba los mismos valores: verificado, sin cambios.
- **Tutorial** con módulos de Clientes, Reportes y Equipo (filtrados por rol) y accesos con clave por chip en el login.
- **Tests:** 36/36 + flujo punta a punta (un estado imposible como `VOLANDO` se rechaza con campo `estado`).

### 🎓 Fase 8: el sistema enseña solo + docs para defenderlo

- **Glosario en línea:** términos con `data-g` (OT, RUC, IGV, kardex, merma…) muestran definición al pasar el cursor y al hacer clic; glosario completo de 12 términos en el Tutorial.
- **Caso guiado:** 8 pasos de recepción a entrega con enlaces y progreso por rol guardado.
- **Tours que faltaban:** equipo, clientes y reportes ya tienen recorrido (botón ¿Cómo usar?) + bienvenida la primera vez que entras por rol.
- **Docs:** `docs/GUIA_DESARROLLO.md` (fases, Mermaid de flujo/entidades/secuencia, decisiones), `docs/MANUAL_USUARIO.md` (tareas por rol) y `docs/PREGUNTAS_PROFE.md` (14 respuestas para la defensa).

### 📊 Fase 7: los números salen de la BD, no de la memoria

- **Indicadores corregidos:** OT del mes filtra por `fechaFin` del mes e ingresos = comprobantes EMITIDO cobrados (antes contaba todo el historial y sumaba cotizaciones). El dashboard mejora solo.
- **Zona horaria única:** `util/AppTime` (America/Lima) para todo "ahora" del negocio; el test de flujo atrapó el desfase UTC/Lima antes que un usuario.
- **16 endpoints:** clientes-por-día (+CSV), ingresos día/mes, comprobantes-resumen, recepciones-por-estado, servicios top, rendimiento de mecánicos (con nombre completo), stock-bajo, inventario resumen/consumo/kardex/entradas, gastos, resultado con fórmula visible, más usados y margen. Permisos: ADMIN todo, RECEPCIONISTA clientes, ALMACENERO stock.
- **Pantalla Reportes:** 7 pestañas filtradas por rol, filtros Hoy/7d/Mes/Mes-anterior, KPIs, gráficos SVG propios (cero dependencias, funcionan sin internet), tablas con CSV, kardex por producto, registro de gastos y recuadro "¿Cómo leer?" en cada pestaña.
- **Tests:** 36/36, incluyendo flujo recepción→diagnóstico→cotización→OT→comprobante→reportes que verifica clientes, ingresos y rendimiento de punta a punta.

### 📦 Fase 6: por fin se sabe qué se gasta y qué se añade

- **Movimientos con foto completa:** cada uno guarda stock antes/después, costo, proveedor, OT, documento y quién lo hizo. El `AJUSTE` viejo se partió en positivo/negativo y se sumó `MERMA` (los `AJUSTE` históricos migran a positivo en V5).
- **Compras reales:** formulario multi-línea con proveedor (RUC validado, creable al vuelo), documento y costo por línea; actualiza stock + último costo. Sin costo no hay compra.
- **Mermas auditadas:** negativo y merma exigen motivo; nada baja de 0; todo con tu usuario.
- **Consumo unificado:** la OT usa `registrarConsumoOT` (OT + costo del momento + usuario). `Producto` suma costo, unidad de medida y activo; la tabla muestra precio, costo y margen.
- **Gastos operativos:** CRUD de proveedores (ADMIN/ALMACENERO) y `gasto` por categorías con `POST/GET /api/gastos` (solo ADMIN). El reporte de resultado llega en Fase 7.
- **Inventario con pestañas:** Stock · Movimientos (filtros + paginado + motivo en tooltip) · Compra · Mermas · Proveedores · Alertas. El mecánico ve catálogo de lectura.
- Los tests de `InventarioService` confirman: sin costo no hay compra, sin motivo no hay merma, consumo guarda OT + usuario + costo del momento, y el kardex cuadraría con `stockAntes/stockDespues`.

### 🔧 Fase 5: el taller se ve como el taller

- **OT en kanban:** 4 columnas (Pendiente → En proceso → En prueba → Finalizada) con contadores, tarjetas con placa/cliente/mecánico/problema y buscador. Avanzar pide confirmación que explica consecuencias (finalizar habilita el cobro). Botón "Cobrar →" directo a caja; modal de pago antiguo con monto manual eliminado (`/pagos` y `/pago-entrega/completa` marcados `@Deprecated`, nadie los usa).
- **Reasignar mecánico:** `PUT /ordenes-trabajo/{id}/mecanico` (ADMIN/RECEPCIONISTA, nunca en OT cerrada ni a no-mecánicos) con modal y lista por carga. Por fin se puede liberar a un mecánico antes de desactivarlo.
- **Cotización en 3 pasos:** wizard Diagnóstico → Cotización → Aprobar; la sección de cotizar se atenúa hasta registrar el diagnóstico, el ID se rellena solo y aprobar explica que nace la OT.
- **Panel del mecánico:** "Hola, Luis. Tienes 3 órdenes", lista priorizada (abiertas primero), tarjetas con vehículo/problema/repuestos usados, botón grande de acción y modal de repuesto con stock visible (sin stock se deshabilita).
- **Unificación real:** el consumo de stock vive en un solo método `consumirProducto` (antes dos bloques idénticos); el registro con costo/OT/usuario llega en Fase 6. `OrdenTrabajoResponseDTO` suma `problemaReportado` y nombre completo.
- **Tests:** 29/29 verdes (consumo único, sin stock sin tocar nada, reasignaciones bloqueadas y nombre completo en DTO).

### 🧾 Fase 4: recibir un auto ya no es un formulario eterno

- **Wizard en 4 pasos** (reusa el patrón `ag-wz-*` del pago): 1 Cliente (buscador con debounce + tarjeta con vehículos/visitas/última visita, o alta con documento validado en vivo) → 2 Vehículo (elegir de su lista o nuevo con placa validada) → 3 Problema (descripción, combustible, km, daños, accesorios) → 4 Confirmar (resumen + Registrar). No avanza inválido, conserva todo al retroceder, avisa antes de salir con datos sin guardar y al terminar sugiere ir al diagnóstico.
- **Backend de apoyo:** `POST /recepciones/completa` acepta `clienteId`/`vehiculoId` ya verificados; nuevos campos (combustible, daños, accesorios, km, migración V4); `PATCH /clientes/{id}/activo` (soft delete) y `GET /clientes/{id}/resumen` (ficha con visitas, vehículos, recepciones y comprobantes).
- **Panel Clientes (ADMIN, RECEPCIONISTA):** buscador instantáneo + filtros por tipo/estado, paginación del servidor, ficha completa, crear/editar con RUC validado, activar/desactivar sin borrar y "Nueva recepción" con el cliente precargado (`?clienteId=`).
- **Roles/nav:** `clientes` en `ROLE_PAGES`, `ROLE_NAV`, buscador Ctrl+K, guías y dashboard. Tour de recepción actualizado a los nuevos selectores.
- **Tests:** 24/24 verdes (resumen con visitas/última visita/folio, desactivar sin borrar, wizard con IDs verificados, placa ajena bloqueada).
- **Hotfix:** `recStep` expuesta en `window` (el wizard la llama desde `onclick` en línea y vivía dentro del IIFE → `ReferenceError`) y `loadPanel` global para el botón Reintentar del mecánico. Auditoría completa: ningún otro `onclick` huérfano en las 11 páginas.
- **Hotfix 400 fantasma:** registrar recepción con cliente existente moría con `Error 400` sin mensaje. Causas: (1) el DTO exigía `clienteNombre`/`vehiculoPlaca` aunque vinieran `clienteId`/`vehiculoId` (ahora solo se exigen al crear, con error claro); (2) los `""` de email/teléfono chocaban con `@Email` (nuevo `agLimpia()` los quita antes de enviar; aplicado en recepción, clientes y proveedores); (3) `apiFetch` ya muestra los mapas `{campo: mensaje}` de validación en vez de `Error 400` seco. Tests 39/39.

### 🔧 Fase 3: se acabó "Mecánico Uno"

- **Personas reales:** `usuario` ahora tiene nombres, apellidos, DNI, teléfono, especialidad y fecha de ingreso (migración V3). El `nombre` es el nombre completo y es lo que muestran OT, paneles, comprobantes ("Atendido por …") y reportes.
- **Equipo inicial:** Luis Ramírez (Motor), Jorge Castillo (Frenos y suspensión), Marco Delgado (Electricidad) + admin/almacén/recepción con nombre y apellido. Las BD viejas se reparan solas al arrancar (sin tocar claves).
- **Selects vivos:** cotización y OT cargan `GET /usuarios/mecanicos` ordenados por menor carga, con "Sugerido" al menos ocupado. Cero `<option value="2">` hardcodeados.
- **Pantalla Equipo (solo ADMIN):** avatar de iniciales, rol, especialidad, contacto, carga (OT activas + finalizadas del mes); crear con DNI validado en vivo, editar, desactivar sin borrar, restablecer clave.
- **Reglas que protegen:** email y documento únicos; no desactivarte a ti mismo; nunca quedarse sin ADMIN; mecánico con OT en proceso exige reasignar primero.
- **API:** `GET/POST /api/usuarios`, `PUT /{id}`, `PATCH /{id}/activo`, `POST /{id}/restablecer-clave`, `GET /mecanicos`, `GET /yo` + `nombreCompleto` en el login.
- **Tests:** 20/20 verdes. Nota: las claves demo siguen `admin123` a propósito; la rotación a secretos por entorno va en Fase 9.

### 👥 Fase 2: el teléfono ya no identifica a nadie

- **Identidad real:** `findOrCreateCliente` ahora busca solo por `(tipo_documento, documento)`. Dos personas con el mismo teléfono o email jamás se fusionan. Sin documento, se crea cliente nuevo.
- **Placa con dueño:** si la placa existe y es de otro cliente → 409 ("ya está registrada a nombre de …"). Una placa, un dueño.
- **Validación que sí ejecuta:** `@Valid` en clientes, vehículos y recepciones (antes las anotaciones existían pero ningún controller las activaba). `ClienteRequest` valida tipo, documento cruzado (RUC exige razón social + dirección fiscal), celular `9XXXXXXXX` y email; `VehiculoRequest` valida placa peruana y año 1950–2100.
- **Endpoints nuevos:** `GET /api/clientes/buscar?q=&page=` (documento, nombre, teléfono, email, razón social), `GET /api/clientes/{id}/vehiculos`, `PUT /api/clientes/{id}` (con chequeo de duplicado). Errores `{mensaje, campo, sugerencia}` con 404/409 donde toca.
- **Limpieza:** fuera el `try/catch(Exception)` de `crear`; `RuntimeException`/`IllegalArgumentException` → `BusinessException`.
- **Tests:** 13/13 verdes, incluyendo no-fusión por teléfono y bloqueo de placa ajena.

### 🗄️ Flyway: adiós al `missing column` en bucle

- **Problema:** con el volumen `pgdata` ya creado, Postgres no vuelve a correr `init.sql` y `ddl-auto=validate` tumbaba el backend (`missing column [activo] in table [cliente]`, reinicio infinito). Solo quedaba ALTER a mano o `down -v` perdiendo datos.
- **Fix permanente:** Flyway es ahora el único dueño del esquema (`db/migration/postgresql` y `db/migration/h2`, V1 base + V2 comprobantes). Al arrancar, en volumen viejo hace baseline de V1 y aplica solo lo pendiente; en volumen nuevo migra todo. `init.sql` quedó mínimo y `schema-h2.sql` se eliminó para que no haya tres fuentes de verdad.
- **Regla nueva:** cada cambio de entidad va en una migración `V<N>__...sql` por perfil. Nunca se edita una migración ya aplicada (Flyway valida checksums).
- Verificado con `mvn test`: 10/10 verdes, log muestra `Migrating schema PUBLIC to version 1/2`.

### 🧾 Fase 1: comprobantes legítimos en el backend (prioridad del docente)

Se acabó el `localStorage` para datos de negocio: series, folios, IGV y snapshots viven en la BD con numeración atómica.

| Cambio | Detalle |
| :-- | :-- |
| BD | `serie_comprobante` + `comprobante` + `comprobante_detalle`; `cliente` con tipo de documento, razón social, dirección, activo; `pago_entrega.comprobante_id`; índice único parcial (una OT, un EMITIDO). Migración `2026-10-01_comprobantes.sql` + `init.sql` + `schema-h2.sql` sincronizados |
| Validadores | `DocumentoValidator` (RUC con prefijo + dígito verificador, DNI/CE/pasaporte) y `NumeroALetras` (`SON: … SOLES`), ambos con tests JUnit 5 |
| Emisión | `POST /api/comprobantes` en una sola transacción: OT finalizada + sin comprobante vigente, monto desde la cotización (lo del navegador se ignora), serie con bloqueo pesimista, snapshots de emisor/adquirente, detalle con valor sin IGV, `total = gravado + IGV` al centavo, pago enlazado + entrega opcional |
| Reglas | Factura exige RUC válido + razón social + dirección; boleta > S/ 700 exige DNI; RUC no puede ir en boleta; crédito solo en factura; anular (solo ADMIN) no libera número y permite re-emitir |
| API nueva | `POST/GET /api/comprobantes`, `GET /previsualizar`, `POST /{id}/anular`, `GET /ordenes-trabajo/{id}/comprobante`, `GET /api/documentos/validar` (✔/✖ en vivo), errores `{mensaje, campo, sugerencia}` |
| Frontend | Wizard consume la API (adiós `ag_comprobantes_v1`); ticket y PDF con emisor, detalle, letras, QR y leyenda académica; historial con filtros; caja abierta a RECEPCIONISTA |
| Tests | 10/10 verdes (`mvn test`): validadores, letras y arranque completo del contexto H2 |

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## 🔴 1.4.2 — 2026-10-01

<img src="./readme-assets/ver-142.svg" width="360" alt="v1.4.2 pdf real sin encabezados" />

### 📄 Descargar baja un PDF de verdad + imprimir sale limpio

- **Problema 1:** al imprimir aparecian el titulo/URL de la pagina arriba y `1/1 + fecha` abajo. Eso lo pone el navegador, no el ticket. **Fix:** `@page { margin: 0 }` (con eso Chrome/Edge ya no imprimen su encabezado ni pie) + relleno propio en el area de impresion.
- **Problema 2:** Descargar abria un `blob:` en el navegador en vez de un archivo. **Fix:** boton Descargar ahora genera un **PDF real** (`BOLETA-B001-000001.pdf`) con generador propio en JS, sin librerias ni internet: ticket de 80mm con cabecera oscura, banda de folio, bloque cliente, concepto, Subtotal/IGV/Total y sello PAGO REGISTRADO. Estructura verificada en Node (cabecera `%PDF-1.4`, xref, trailer, `%%EOF` y longitud del stream exactas, con Ñ y tildes).
- La constancia queda con **Cerrar / Descargar PDF / Imprimir**, las tres fieles al diseño de pantalla.

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## 🔵 1.4.1 — 2026-10-01

<img src="./readme-assets/ver-141.svg" width="360" alt="v1.4.1 impresión fiel y descarga" />

### 🎫 Lo impreso sale igual que en pantalla + se puede descargar

- **Problema:** al imprimir, la constancia salia sin los colores (cabecera oscura en blanco). Causa: los navegadores quitan los fondos al imprimir por defecto.
- **Fix impresion fiel:** `print-color-adjust: exact` en el ticket y su contenido (`polish.css` + reglas `@media print` de la pagina), regla `@page` con margen de 8mm y bloques que no se cortan a mitad (`break-inside: avoid`).
- **Boton Descargar:** la constancia suma **Cerrar / Descargar / Imprimir**. Descargar baja un archivo `BOLETA-B001-000001.html` autocontenido (estilos en linea, sin internet) que se ve identico y sirve para enviar por WhatsApp/correo o guardar como PDF desde el navegador.
- Sintaxis verificada con Node y llaves CSS balanceadas.

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## 🟠 1.4.0 — 2026-10-01

<img src="./readme-assets/ver-140.svg" width="330" alt="v1.4.0 rediseño visual guiado" />

### ✨ Todo se ve mejor y se entiende solo

Nueva capa `frontend/css/polish.css` (cargada en las 10 paginas despues de `redesign.css`): solo suma, no rompe el diseño existente.

| Eje | Que cambio |
| :-- | :-- |
| 🧭 Guiado | Banner contextual en cada pagina ("que hago aqui" en 3 pasos + boton al siguiente paso). Se oculta con la X y recuerda tu eleccion. El CTA respeta tu rol (nunca te manda a una pagina prohibida) |
| 📐 Simetria | Nuevo componente `.ag-head` (icono + titulo + meta alineados) aplicado a Pago y Entrega; modales con header/body/footer reales (esas clases existian en el HTML pero sin CSS) |
| 📱 Responsive | Grillas de 4 → 2 columnas en tablet; sidebar apilado <1100px; **navbar movil con scroll horizontal** (antes el menu desaparecia y no habia como navegar); modales tipo hoja inferior; tablas ya tenian scroll |
| ⚡ Dinamismo | Resumen del dia con conteo animado, badges de estado, hovers con elevacion, foco visible para teclado y respeto a `prefers-reduced-motion` |
| 🧾 Wizard de pago | Cobrar ahora es Paso 1 Comprobante (tarjetas Boleta/Factura seleccionables) → Paso 2 Cliente y pago (errores en linea, sin popups) → Paso 3 Confirmar (revision + Confirmar pago). Vista previa del ticket siempre visible al costado |
| 🎫 Ticket nuevo | Constancia profesional: cabecera oscura con marca + folio + fecha, bloque cliente, concepto, Subtotal/IGV/Total destacados y sello PAGO REGISTRADO. Impresion limpia solo-ticket |
| 🧹 Detalles | Dashboard suma `ui-kit.js` (toasts, Ctrl+K, guia); estado vacio "Todo cobrado y entregado" con CTA; `headTotal` con lo por cobrar en el encabezado |

> Sintaxis JS verificada con Node (`ui-kit.js` + wizard de pago) y llaves CSS balanceadas.

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## 🔴 1.3.1 — 2026-10-01

<img src="./readme-assets/ver-131.svg" width="300" alt="v1.3.1 fix de pago y paneles por rol" />

### 🐛 Fix crash al emitir boleta/factura

- **Error:** `can't access property "otId", pagoActual is null` al confirmar el pago. Causa: `confirmarPago()` en `pago_entrega.html` llamaba a `cerrarPagoModal()` (que pone `pagoActual` en `null`) y despues intentaba leer `pagoActual.otId` para mostrar la constancia.
- **Fix:** se capturan `otId` y `monto` en variables locales antes de cerrar el modal; la constancia usa esas copias. Sintaxis verificada con Node.

### 🧍 Paneles exclusivos por rol + rediseño

| Cambio | Detalle |
| :-- | :-- |
| Admin limpio | `ROLE_NAV` del ADMIN ya no incluye `mecanico` ni `recepcionista`; `ROLE_PAGES` deja esos paneles solo para `MECANICO` y `RECEPCIONISTA`. Si el admin entra por URL directa, ve el 403 |
| Panel Mecanico | Header con badge `MECANICO` + saludo personal (`Hola, {nombre}`); accesos rapidos corregidos a sus paginas reales (Mis Ordenes, Inventario, Tutorial). Antes enlazaba a Recepcion y Cotizacion, que le devolvian 403 |
| Panel Recepcionista | Header con badge `RECEPCIONISTA` + saludo personal; accesos rapidos corregidos (Nueva Recepcion, Nueva Cotizacion, Ordenes de Trabajo, Tutorial). Antes enlazaba a Pagos y Entregas, que le devolvia 403 |
| Bypass eliminado | Ambos paneles quitaron el chequeo extra que dejaba pasar al ADMIN; `guardPage()` es la unica puerta |

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## 🟢 1.3.0 — 2026-10-01

<img src="./readme-assets/ver-130.svg" width="520" alt="v1.3.0 actual, frontend hecho, backend pendiente" />

### 🧾 Boleta / Factura al pagar (con constancia para el usuario)

Pagar ya no es un boton a ciegas: ahora eliges el comprobante y te llevas tu constancia impresa.

| Paso | Que pasa |
| :-- | :-- |
| 1 | Clic en **Registrar Pago** → se abre el modal |
| 2 | Eliges **🧾 Boleta** (DNI 8 + nombre) o **🏢 Factura** (RUC 11 + razon social + direccion fiscal) |
| 3 | Eliges metodo: EFECTIVO · YAPE · PLIN · TRANSFERENCIA · TARJETA |
| 4 | Ves el desglose auto: Subtotal + IGV 18% + Total |
| 5 | Confirmas → se registra el pago y se abre tu **constancia** (serie `B001`/`F001` + correlativo) |
| 6 | La tarjeta muestra el badge `BOLETA B001-000123` y el boton **Ver comprobante** reimprime cuando quieras |

> [!NOTE]
> La constancia se guarda en el navegador (`localStorage`) y se imprime con el boton **Imprimir**. Cierra con X, clic fuera o Esc.

> [!WARNING]
> Comprobante **interno imprimible**, NO facturacion electronica SUNAT (sin PSE/OSE, QR ni envio a SUNAT). El comprobante vive en el navegador donde se emitio hasta que el backend lo persista (ver pendiente ⏳ abajo).

<details open>
<summary><b>🔧 Detalle tecnico v1.3.0</b></summary>

<br>

**Modal de pago (`frontend/pages/pago_entrega.html`):**

- `abrirPagoModal()` con radio BOLETA/FACTURA, documento validado por tipo, select de metodo y desglose auto Subtotal (`total/1.18`) + IGV 18% + Total con vista previa de serie.
- Al confirmar: POST existente (`/pago-entrega/completa`) + correlativo por serie en `localStorage` (`ag_comprobantes_v1` / `ag_correlativos_v1`) + ticket con serie-numero, fecha, DNI/RUC, cliente, metodo y totales (`window.print()` con `@media print` solo-ticket).
- Pagos anteriores a v1.3.0 muestran constancia generica reimprimible. Paso 2 del Flujo de Pago renombrado a "Pago + Comprobante".

**⏳ Pendiente backend (gap real):**

- `pago_entrega` aun solo guarda `monto` + fechas (`entity/PagoEntrega.java`, `service/PagoEntregaService.java`, `database/init.sql`).
- Falta migracion `2026-10-01_comprobantes.sql` (`cliente`: `tipo_documento`, `razon_social`, `direccion_fiscal`; `pago_entrega`: `metodo_pago`, `tipo_comprobante`, `serie`, `numero` + `UNIQUE(serie, numero)`, `subtotal`, `igv`, `total`, snapshots, `estado_comprobante`) y payload extendido en `PagoRequest` + `GET /api/comprobantes/{id}`.

</details>

### 📁 Reorden de carpetas a la raiz + rutas corregidas

| Cambio | Detalle |
| :-- | :-- |
| Estructura plana | El codigo subio de `sistema-de-autogestion/sistema-de-autogestion/` a la raiz (`backend/`, `frontend/`, `database/`, `README.md`, `CHANGELOG.md`, `docker-compose.yml`, `readme-assets/`) |
| README | Arbol reescrito (incluye `redesign.css`, `ui-kit.js`, `confirm-modal.js`, `icons.js`, `migrations/`, paneles y `acceso_denegado.html`); comando `docker compose up --build` sin `cd`; puerto DB `5433 (host) → 5432 (contenedor)` |
| Cache-busting | `orden_trabajo.html` y `pago_entrega.html` cargaban `styles.css?v=20260920b?v=20260913b` (doble `?v=`); unificado a `?v=20261001` |
| Navbar (`frontend/js/api.js`) | Nuevo `agIsInPages()` / `agLoginPath()`; `logout()` y `checkAuth()` ya no asumen `../index.html`; el logo ya no apunta a `pages/pages/dashboard.html` |
| `database/Dockerfile` | Ademas de `init.sql` copia `migrations/*.sql` al entrypoint de Postgres |

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## 🟡 1.2.0 — 2026-09-20

<img src="./readme-assets/ver-120.svg" width="440" alt="v1.2.0 roles, seguridad backend, UI stepper" />

> Cada usuario ve solo sus paneles, el backend tambien valida roles y los numeritos ya tienen botones propios.

<details open>
<summary><b>🛡️ Control de acceso por rol (cada usuario ve solo sus paneles)</b></summary>

<br>

- **Mapa centralizado de permisos en `js/api.js`:** nuevo `ROLE_PAGES` (paginas permitidas por rol) y `ROLE_NAV` (links del navbar por rol), con los helpers `guardPage()` (valida sesion + rol y redirige al panel propio), `canSee()` y `homePageForRol()`.
- **Navbar por rol:** cada usuario solo ve los enlaces de sus modulos; el logo lleva a la pagina de inicio de su rol.
- **Login con redireccion por rol:** Admin → Dashboard, Mecanico → Panel Mecanico, Recepcionista → Panel Recepcionista, Almacenero → Inventario.
- **Guard de rol en las 8 paginas** (dashboard, recepcion, cotizacion, orden_trabajo, inventario, pago_entrega, mecanico, recepcionista): quien entra por URL a una pagina no permitida es devuelto a su panel de inicio. Antes solo mecanico y recepcionista validaban rol, y varias paginas no validaban nada.
- **Acciones sensibles ocultas por rol:**
  - Crear ordenes de trabajo: solo ADMIN y RECEPCIONISTA.
  - Registrar pago y entrega del vehiculo: solo ADMIN.
  - Inventario: escritura (nuevo producto, entradas, ajustes de stock) solo para ALMACENERO y ADMIN; MECANICO ve el catalogo en solo lectura.
  - El ADMIN ahora puede abrir los paneles de mecanico y recepcionista (se relajo su validacion interna, que expulsaba a quien no fuera el dueño del panel).
- **Buscador global Ctrl+K y Tutorial:** filtran sus modulos y tarjetas segun el rol.
- **CSS:** regla global `[hidden] { display: none !important; }` porque `.ag-action-link` usaba `display:flex` y anulaba el atributo `hidden` al ocultar acciones por rol.

</details>

<details>
<summary><b>🔒 Seguridad en el backend (roles por endpoint)</b></summary>

<br>

- **`SecurityConfig`:** todas las rutas `/api/**` ahora exigen autenticacion (`authenticated()`); solo `/api/auth/**` (login), la consola H2 y los preflight OPTIONS quedan abiertos. Punto de entrada que responde **401** para peticiones sin token.
- **`@RolesAllowed` en los controladores:** los endpoints validan el rol ademas de la UI:
  - Clientes, Vehiculos, Recepciones, Diagnosticos, Cotizaciones y Ordenes de trabajo (lectura): ADMIN / RECEPCIONISTA / MECANICO.
  - Crear OT (`POST /ordenes-trabajo` y `/completa`): solo ADMIN / RECEPCIONISTA.
  - Cambiar estado de OT y registrar productos usados: solo ADMIN / MECANICO.
  - Inventario: lectura para ADMIN / ALMACENERO / MECANICO; escritura (crear producto, movimientos, actualizar) solo ADMIN / ALMACENERO.
  - Pagos y entregas (pagos, entregas, pago-entrega/completa, finalizadas-completa, monto, consulta de pago): solo ADMIN.
  - Reportes/indicadores: ADMIN / ALMACENERO. Servicios (catalogo): todos los roles.
  - `AuthController` (login) queda abierto por diseño. Se eliminaron los `@PermitAll` sueltos y el `@PreAuthorize("permitAll()")` que dejaban rutas sin proteccion.
- **`GlobalExceptionHandler`:** nuevo handler de `AccessDeniedException` que responde **403** con mensaje claro (antes caeria en el handler de `RuntimeException` y saldria como 400).
- **`apiFetch` distingue 401/403:** un 401 cierra sesion y manda al login; un 403 solo informa el error de permisos (ya no expulsa al usuario de su sesion).
- **Nueva pagina `pages/acceso_denegado.html`:** pantalla "Error 403 / Acceso denegado" con el rol actual y botones "Ir a mi panel" (dinamico segun rol) y "Cerrar sesion". La usan `guardPage()`, los paneles de mecanico/recepcionista y la validacion interna del tutorial; sin sesion activa, la propia pagina devuelve al login.
- **Verificacion:** build del backend OK (Maven dentro de Docker) y sintaxis JS verificada con Node.

</details>

<details>
<summary><b>🛠️ Correcciones de UI y base de datos</b></summary>

<br>

- **Chips de filtro amontonados (Recepcion, Ordenes):** `.ag-section-header` no envolvia contenido; con titulo + 6 chips, los chips se apilaban en columna y el titulo quedaba suelto. Ahora el header hace `flex-wrap` con `gap`.
- **Listas que "aparecian y desaparecian":** `.ag-state-card` tenia `opacity: 0` como estilo base con la animacion `agFadeUp` en modo `backwards` (sin `forwards`); al terminar la animacion, la tarjeta volvia a `opacity: 0` y parecia ocultarse. Corregido a `forwards` (afecta a recepciones, cotizaciones existentes, ordenes y pagos).
- **Error al crear OT (`cotizacion_estado_check`):** `init.sql` nunca recibio los estados nuevos de cotizacion (`EN_DIAGNOSTICO`, `CONVERTIDA`); como el backend corre con `ddl-auto=validate`, la constraint viva de PostgreSQL rechazaba el `UPDATE` a `CONVERTIDA` con un error 400. `init.sql` corregido para instalaciones nuevas y se agrego `database/migrations/2026-09-20_cotizacion_estados.sql` para volumenes de datos existentes.
- **Verificado end-to-end** con el stack levantado en Docker: recepcion → diagnostico → cotizacion → aprobar → crear OT OK (la cotizacion queda `CONVERTIDA`), un almacenero recibe 403 al intentar crear OT y una peticion sin token recibe 401.

</details>

<details>
<summary><b>🔢 UI: stepper de cantidad propio</b></summary>

<br>

- **Fuera spinners nativos:** se ocultaron las flechas nativas de los `input[type=number]` (se veian distintas y feas segun el navegador).
- **Nuevo componente `.ag-stepper`** ( - / input / + ) en el sistema de diseño: botones propios con hover/active/disabled, fondo de foco en el input, soporte de tema oscuro y tres tamanos (`sm`, base, `lg`). Helper `agStepper(id, { value, min, max, step, size })` en `api.js`; los botones respetan min/max/step y disparan eventos `input`/`change`.
- **Aplicado en:** Cantidad Usada del modal de productos usados (Ordenes de trabajo), Cantidad del formulario de movimientos (Inventario) y cantidades de la tabla de productos seleccionables (Cotizacion).
- **Cache busting:** `?v=20260920b` en las referencias a `api.js` y `styles.css` de todas las paginas (dos paginas tenian version vieja y el resto ninguna).

</details>

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## 🔵 1.1.0 — 2026-09-20

<img src="./readme-assets/ver-110.svg" width="330" alt="v1.1.0 rediseño, tutorial y tour" />

> El sistema se viste de gala: nueva capa visual, tutorial interactivo y tour guiado.

<details>
<summary><b>✨ Rediseño visual completo del frontend (clic para ver todo lo que cambio)</b></summary>

<br>

- **Nueva capa de diseño (`css/redesign.css`):** fondo con gradientes ambientales, navbar premium con subrayados animados, botones con degradado y micro-interacciones, tarjetas de estado con riel de color y barra de progreso, filtros tipo chip con contadores en vivo, toasts flotantes, buscador global, tour guiado y estilos del tutorial. Respeta el tema claro/oscuro existente.
- **Nuevo `js/ui-kit.js`:** notificaciones toast (también intercepta `showSuccess`/`showError`), buscador global `Ctrl+K` para saltar a cualquier módulo, botón flotante "¿Cómo usar?" y **tour guiado automático por página** (se muestra una vez; reiniciable desde el botón).
- **Nueva página `pages/tutorial.html`:** tutorial interactivo con flujo del taller paso a paso (clicable), tarjetas por módulo, explicación de roles, checklist de dominio con progreso guardado, mini-quiz con retroalimentación y preguntas frecuentes. Accesible desde el navbar, el login y los paneles.
- **Login (`index.html`):** animaciones de entrada mejoradas, botón para mostrar/ocultar contraseña, chips de acceso rápido con las cuentas demo (admin, mecanico, recepcionista, almacen) y enlace al tutorial.
- **Dashboard:** KPIs con contador animado, flujo del taller interactivo (clic en cada paso muestra qué hace), tarjeta destacada hacia el tutorial.
- **Recepción:** filtros convertidos a chips con contadores por estado, tarjetas con riel de estado y barra de progreso del flujo, skeletons de carga y empty states.
- **Cotización:** resumen de total en caja destacada con animación, chips de items seleccionados con eliminación rápida, listado de cotizaciones con tarjetas de estado, Enter agrega servicios.
- **Inventario:** buscador instantáneo de productos, barra visual de stock por producto (rojo/verde), alertas de stock rediseñadas, modales con cierre por Esc y clic fuera.
- **Órdenes de trabajo:** tarjetas con barra de progreso por estado, chips de filtro con contadores, eliminados `console.log` de depuración y corregido un `<span>` mal cerrado; ya no depende de colores fijos para tema claro.
- **Panel Mecánico:** tarjetas de OT con progreso y botones con confirmación toast.
- **Pago y Entrega:** tarjetas rediseñadas con monto destacado y badges de pago/entrega; corregido bug de template string en la confirmación de entrega.
- **Panel Recepcionista:** ahora muestra estadísticas reales (recepciones activas, cotizaciones pendientes/aprobadas), últimas cotizaciones y accesos rápidos.
- **Navbar:** enlace fijo a **Tutorial** para todos los roles + botón de búsqueda.

</details>

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

## ⚪ 1.0.0 — 2026-09-14

<img src="./readme-assets/ver-100.svg" width="150" alt="v1.0.0 base" />

> La base que sostiene todo: se acabaron los errores 500 fantasma, los pagos duplicados y el caché terco.

<details>
<summary><b>🐛 Correccion de errores — Backend (clic para ver el detalle)</b></summary>

<br>

**LazyInitializationException (Error 500)**

- **Problema:** Las entidades con relaciones `@ManyToOne(fetch = FetchType.LAZY)` provocaban `LazyInitializationException` al serializar la respuesta a JSON fuera del alcance de una transacción.
- **Solución:** Se crearon DTOs para las entidades y se modificaron los servicios para devolver DTOs en lugar de entidades directamente. También se agregó `@Transactional(readOnly = true)` a los métodos de lectura y se realizó el mapeo de las entidades a DTOs dentro de los límites transaccionales.
- **Archivos modificados:**
  - `DiagnosticoResponseDTO.java` (nuevo)
  - `VehiculoResponseDTO.java` (nuevo)
  - `RecepcionResponseDTO.java` (nuevo)
  - `OrdenTrabajoResponseDTO.java` (nuevo)
  - `OtProductoUsadoResponseDTO.java` (nuevo)
  - `PagoEntregaResponseDTO.java` (nuevo)
  - `CotizacionResponseDTO.java` (actualizado)
  - `DiagnosticoService.java` - Se agregó `@Transactional` a los métodos públicos y ahora devuelve DTOs.
  - `CotizacionService.java` - Se agregó `@Transactional(readOnly = true)` a los métodos de lectura y ahora devuelve DTOs.
  - `VehiculoService.java` - Se agregó `@Transactional` a los métodos y ahora devuelve DTOs.
  - `RecepcionService.java` - Se agregó `@Transactional` a los métodos y ahora devuelve DTOs.
  - `PagoEntregaService.java` - Se agregó `@Transactional(readOnly = true)` a los métodos de lectura y ahora devuelve DTOs.
  - `OrdenTrabajoService.java` - Se agregó `@Transactional(readOnly = true)` a los métodos de lectura y ahora devuelve DTOs.
  - Todos los controladores fueron actualizados para devolver DTOs en lugar de entidades.

**Violación de restricción por pago duplicado (400)**

- **Problema:** Registrar un pago duplicado para la misma OT provocaba un error 500 debido a la violación de una restricción única.
- **Solución:** Se modificó `registrarPagoInterno()` en `OrdenTrabajoService` para comprobar si ya existe un pago y mostrar un mensaje de error claro cuando se intenta registrar uno duplicado.
- **Archivo modificado:** `OrdenTrabajoService.java`

**Gestión del estado de las cotizaciones**

- **Problema:** Las cotizaciones permanecían en estado `APROBADA` después de crear una OT, permitiendo crear múltiples OT a partir de la misma cotización.
- **Solución:** Se agregó la transición de estado a `CONVERTIDA` después de crear la OT, tanto en `crear()` como en `crearCompleta()`.
- **Migración de base de datos:** Se agregó `CONVERTIDA` a la restricción `cotizacion_estado_check`.
- **Archivos modificados:** `OrdenTrabajoService.java` y migración de base de datos.

**Optimización de consultas en `CotizacionRepository`**

- **Problema:** `findAll()` provocaba `LazyInitializationException` debido a relaciones `LAZY` anidadas.
- **Solución:** Se agregaron consultas con `LEFT JOIN FETCH` para las relaciones `diagnostico`, `recepcion`, `vehiculo`, `cliente` y `mecanico`.
- **Archivos modificados:** `CotizacionRepository.java`, `VehiculoRepository.java`, `DiagnosticoRepository.java`

**PagoEntregaRepository**

- Se agregó `LEFT JOIN FETCH` para la relación `ordenTrabajo`.

**PagoEntregaService**

- Se agregó `@Transactional(readOnly = true)` al método `obtenerMonto()`.

**GlobalExceptionHandler**

- Se agregó un manejador para `RuntimeException` con el mensaje de respaldo: `"Error inesperado, contacte soporte"`.

**OrdenTrabajoService**

- Se agregó el mapeo mediante `OtProductoUsadoResponseDTO`.
- Se agregó `@Transactional(readOnly = true)` a `listarFinalizadasConPago()`.
- Se hizo público `toResponseDTO()` para permitir su uso desde otros servicios.

</details>

<details>
<summary><b>🖥️ Correccion de errores — Frontend (clic para ver el detalle)</b></summary>

<br>

**Cache Busting**

- Se agregó el parámetro `?v=20260913b` a las referencias de CSS y JS en los archivos HTML.

**LazyInitializationException en el frontend**

- Se corrigió el mapeo de campos en `pago_entrega.html` y `mecanico.html` para utilizar los campos planos de los DTOs en lugar de objetos anidados.

**Race Condition en `loadOrdenes()`**

- Se agregó un control de secuencia mediante `loadOrdenesSeq` para evitar condiciones de carrera.
- Se eliminó `onchange="loadOrdenes()"` del elemento `select` y se reemplazó por `addEventListener`.
- Se agregó el control de secuencia también en el bloque `catch`.

**Problema de alcance con IIFE (ReferenceError)**

- **Problema:** Las funciones definidas dentro de una IIFE no eran accesibles desde los manejadores `onclick` escritos directamente en HTML.
- **Solución:** Se expusieron las funciones necesarias mediante el objeto `window`:
  - `window.aprobarCotizacion`
  - `window.rechazarCotizacion`
  - `window.agregarServicio`
  - `window.agregarProducto`
  - `window.eliminarItem`

**Modal de confirmación personalizado**

- Se creó `frontend/js/confirm-modal.js` con la función reutilizable `confirmarAccion(mensaje, opciones)`.
- La función devuelve `Promise<boolean>` para permitir su uso con `async/await`.
- Se reemplazaron todas las llamadas a `confirm()` nativo en `orden_trabajo.html` y `pago_entrega.html`.
- Se reutilizaron los estilos CSS existentes `.ag-modal-overlay` y `.ag-modal`.

**Cache Busting para recursos estáticos**

- Se agregó el parámetro `?v=20260913b` a las referencias de CSS y JS en todas las páginas HTML.

**Actualización de selects después del envío**

- `orden_trabajo.html`: Se vuelve a cargar `cotizacionSelect` después de crear una OT.
- `cotizacion.html`: Se vuelve a cargar `recepcionSelect` después de crear un diagnóstico.
- Se eliminaron opciones almacenadas en caché que podían provocar envíos duplicados.

**Endpoints de `OtProductoUsado`**

- Se creó `OtProductoUsadoResponseDTO`.
- Se modificaron `registrarProductoUsado()` y `listarProductosUsados()` para devolver DTOs.
- Se agregó el método auxiliar `toProductoUsadoResponseDTO()`.

**GlobalExceptionHandler**

- Se agregó un manejador para `RuntimeException` con el mensaje de respaldo: `"Error inesperado, contacte soporte"`.
- Se estableció el orden correcto de los manejadores, colocando los específicos antes del genérico.

</details>

<details>
<summary><b>🗄️ Correccion de errores — Base de datos</b></summary>

<br>

- Se agregó `CONVERTIDA` a la restricción `cotizacion_estado_check`.
- Se agregó `LEFT JOIN FETCH` a `PagoEntregaRepository.findByOrdenTrabajoId()`.
- Se agregó `LEFT JOIN FETCH` a `OtProductoUsadoRepository.findByOrdenTrabajoId()`.

</details>

### ⏳ Problemas conocidos / Mejoras futuras

- Considerar la creación de excepciones específicas como `NotFoundException` y `ConflictException` para utilizar códigos HTTP más precisos, como 404 y 409.
- Actualmente, todas las `RuntimeException` se responden con código HTTP 400.

<br>

<img src="./readme-assets/divider.svg" width="100%" height="6" alt="" />

<div align="center">

**AutoGestion · Taller Mecanico Automotriz**
<br>
[⬆ Volver a versiones](#️-versiones)

</div>
