<div align="center">

![AutoGestion - Sistema de Gestión para Taller Mecánico](readme-assets/banner.svg)

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

**Sistema web de gestión de servicios e inventario para un taller mecánico automotriz.**
Interfaz de estilo industrial (acero, aceite, señalización), modo oscuro y animaciones fluidas.

</div>

![](readme-assets/divider.svg)

## 📑 Contenido

| | | |
|:--|:--|:--|
| [📁 Estructura](#-estructura-del-proyecto) | [🧰 Stack](#-stack-tecnológico) | [🚀 Cómo ejecutar](#-cómo-ejecutar) |
| [🎭 Casos de uso](#-modelo-de-caso-de-uso-de-negocio) | [🔑 Credenciales](#-credenciales-de-prueba) | [🗄️ Datos semilla](#️-datos-semilla) |
| [🔄 Flujo del negocio](#-flujo-del-negocio) | [🧾 Facturación](#-facturación-boletafactura) | [🖥️ Pantallas](#️-pantallas-del-sistema) |
| [🌐 Portal del cliente](#-portal-del-cliente) | [🔒 Seguridad](#-seguridad) | [🎨 Tema y animaciones](#-tema-y-animaciones) |
| [🏗️ Arquitectura](#️-arquitectura-del-sistema) | [🔌 API REST](#-api-rest-endpoints) | [🧬 Base de datos](#-modelo-de-base-de-datos) |
| [🛠️ Problemas comunes](#️-resolver-problemas-comunes) | [⚠️ Aviso legal](#️-aviso-legal-y-parámetros-pendientes) | [📝 Changelog](CHANGELOG.md) |

![](readme-assets/divider.svg)

## 📁 Estructura del Proyecto

<details>
<summary><b>Ver árbol de carpetas</b></summary>

```
SistemaAutoGestion/
├── README.md
├── CHANGELOG.md
├── PLAN.md
├── .env.example
├── docker-compose.yml
├── readme-assets/                     (banner.svg, divider.svg, flow.svg)
├── docs/                              (guía, manual, permisos, políticas, preguntas de defensa)
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/autogestion/
│       │   ├── AutogestionApplication.java
│       │   ├── config/            (Security, JWT, CORS, DataInitializer, EmpresaProperties)
│       │   ├── entity/            (30 entidades JPA)
│       │   ├── repository/        (18 repositorios Spring Data)
│       │   ├── service/           (14 servicios con lógica de negocio)
│       │   ├── controller/        (17 controladores REST)
│       │   ├── dto/               (37 request/response DTOs)
│       │   └── util/              (AppTime, DocumentoValidator, NumeroALetras, Rangos, TicketPdf)
│       └── resources/db/migration/   (Flyway: V1 esquema inicial, V2 fiscal/inventario/gastos, ..., V5 portal del cliente)
├── database/
│   └── Dockerfile                 (imagen Postgres 16; el esquema lo versiona Flyway)
└── frontend/
    ├── Dockerfile
    ├── nginx.conf
    ├── index.html                 (login)
    ├── css/
    │   ├── styles.css             (diseño principal)
    │   ├── animations.css         (animaciones, texturas, skeletons)
    │   └── redesign.css           (rediseño v1.1.0: gradientes, chips, tour)
    ├── js/
    │   ├── api.js                 (cliente API, navbar, helpers, permisos por rol)
    │   ├── animations.js          (transiciones, ripples, stagger)
    │   ├── ui-kit.js              (toasts, buscador Ctrl+K, tour guiado)
    │   └── confirm-modal.js       (modal de confirmación reutilizable)
    ├── pages/
    │   ├── dashboard.html         (indicadores)
    │   ├── recepcion.html         (recepción de vehículos)
    │   ├── cotizacion.html        (diagnóstico + cotización)
    │   ├── orden_trabajo.html     (órdenes de trabajo)
    │   ├── inventario.html        (gestión de inventario)
    │   ├── pago_entrega.html      (pagos, entregas, boleta/factura)
    │   ├── mecanico.html          (panel mecánico)
    │   ├── recepcionista.html     (panel recepcionista)
    │   ├── tutorial.html          (tutorial interactivo)
    │   ├── acceso_denegado.html   (error 403)
    │   └── politicas/index.html   (índice de políticas)
    └── portal/                    (portal del cliente: estado, cotización, comprobantes,
                                    historial, mantenimiento, cita, encuesta, políticas)
```

</details>

![](readme-assets/divider.svg)

## 🧰 Stack Tecnológico

| Capa | Tecnología |
|:--|:--|
| 🖼️ **Frontend** | HTML + CSS + JavaScript (sin framework) + animaciones CSS propias |
| ⚙️ **Backend** | Java 21 / Spring Boot 3 (Spring Web, Spring Data JPA, Spring Security) |
| 🗄️ **Base de datos** | PostgreSQL 16 (vía Docker) + migraciones Flyway |
| 🔐 **Autenticación** | JWT (JSON Web Tokens) |
| 👥 **Roles** | `ADMIN` · `MECANICO` · `RECEPCIONISTA` · `ALMACENERO` |
| 📦 **Despliegue** | Docker Compose (db + backend + frontend con Nginx) |

![](readme-assets/divider.svg)

## 🚀 Cómo Ejecutar

### Requisitos

- **Docker** → https://docs.docker.com/get-docker/
- **Docker Compose** (incluido con Docker Desktop)

### Paso 1 — Levantar el sistema

```bash
cp .env.example .env   # opcional: ajusta claves y demo (nunca subas .env)
docker compose up --build
```

Se construyen e inician 3 contenedores. Flyway crea o actualiza el esquema al arrancar el backend y conserva tus datos.

| Contenedor | Servicio | Puerto |
|:--|:--|:--|
| `db` | PostgreSQL 16 | 5433 (host) → 5432 (contenedor) |
| `backend` | Spring Boot | 8080 |
| `frontend` | Nginx | 5500 |

> [!NOTE]
> Espera a que los logs de los tres contenedores terminen de iniciar.

### Paso 2 — Abrir el navegador

- **Frontend:** http://localhost:5500
- **Backend API:** http://localhost:8080/api

### Paso 3 — Iniciar sesión

- **Email:** `admin@sanmartin.pe`
- **Contraseña:** `Admin-2026*` (demo; cámbiala con `APP_ADMIN_PASSWORD` en tu `.env`)

### Paso 4 — Recorrer el flujo del negocio

```
Login → Dashboard → Recepción → Cotización → Órdenes de Trabajo → Inventario → Pago/Entrega
```

![](readme-assets/divider.svg)

## 🎭 Modelo de Caso de Uso de Negocio

Actores: **Cliente, Recepcionista, Mecánico, Administrador**.

> El Recepcionista es el actor clave: registra la llegada del cliente y su vehículo, inicia la recepción y coordina el flujo inicial del taller.

![](readme-assets/divider.svg)

## 🔑 Credenciales de Prueba

| Usuario | Email | Contraseña | Rol |
|:--|:--|:--|:--|
| Administrador | `admin@sanmartin.pe` | `Admin-2026*` | `ADMIN` |
| Luis Ramírez (Motor) | `mecanico1@sanmartin.pe` | `Meca-2026*` | `MECANICO` |
| Jorge Castillo (Frenos) | `jcastillo@sanmartin.pe` | `Meca-2026*` | `MECANICO` |
| Marco Delgado (Electricidad) | `mdelgado@sanmartin.pe` | `Meca-2026*` | `MECANICO` |
| Rosa Chávez | `recepcionista@sanmartin.pe` | `Recep-2026*` | `RECEPCIONISTA` |
| Pedro Quispe | `almacen@sanmartin.pe` | `Alma-2026*` | `ALMACENERO` |

> [!WARNING]
> Claves distintas por rol, **solo para demo local**. En producción define las tuyas en `.env` (`APP_ADMIN_PASSWORD`, `APP_SEED_PASSWORD_*`) y pon `APP_SEED_DEMO=false` para no sembrar datos de prueba.

![](readme-assets/divider.svg)

## 🗄️ Datos Semilla

Con `APP_SEED_DEMO=true` (valor demo) se insertan automáticamente estos datos. Con `false` solo se crea el ADMIN de arranque.

| Dato | Cantidad |
|:--|:--|
| 👥 Usuarios | 6 (admin, 3 mecánicos con especialidad, almacén, recepción) |
| 🧑‍💼 Clientes | 5 (3 con DNI + 2 empresas con RUC válido) |
| 🚗 Vehículos | 4 (Corolla, Accent, Sentra, Hilux) |
| 🛠️ Servicios | 8 (cambio de aceite, alineación, frenos, etc.) |
| 📦 Productos | 12 (aceite, filtros, pastillas, discos, etc.) |

> [!IMPORTANT]
> Los datos viven en PostgreSQL dentro del contenedor Docker. Si eliminas el volumen `pgdata` se pierden y, con demo activado, se vuelven a insertar al reiniciar.

![](readme-assets/divider.svg)

## 🔄 Flujo del Negocio

![Flujo del negocio](readme-assets/flow.svg)

```
1. RECEPCIÓN
   └─> Registrar cliente + vehículo + problema reportado

2. DIAGNÓSTICO
   └─> El mecánico describe los hallazgos técnicos

3. COTIZACIÓN
   └─> Seleccionar servicios y productos
   └─> El total se calcula automáticamente
   └─> Aprobar o rechazar

4. ORDEN DE TRABAJO
   └─> Se crea automáticamente al aprobar la cotización
   └─> Se asigna a un mecánico
   └─> Estados: PENDIENTE → EN_PROCESO → EN_PRUEBA → FINALIZADA

5. EJECUCIÓN
   └─> El mecánico registra los productos usados
   └─> El stock se descuenta automáticamente (transacción atómica)

6. PAGO Y ENTREGA
   └─> Se registra el pago (método + comprobante BOLETA / FACTURA)
   └─> Boleta: DNI/CE/PAS + nombre. Factura: RUC 11 dígitos + razón social + dirección fiscal
   └─> Desglose: Subtotal + IGV (18%) + Total, con serie y número correlativo
   └─> La entrega del vehículo queda bloqueada hasta tener pago con comprobante
```

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE
    PENDIENTE --> EN_PROCESO
    EN_PROCESO --> EN_PRUEBA
    EN_PRUEBA --> FINALIZADA
    FINALIZADA --> [*]
```

![](readme-assets/divider.svg)

## 🖥️ Pantallas del Sistema

| Pantalla | Descripción |
|:--|:--|
| 🔐 **Login** | Pantalla dividida: identidad de marca (izq.) + formulario (der.), selector de tema |
| 📊 **Dashboard** | 4 indicadores animados, flujo visual, alertas de stock y accesos rápidos |
| 🧾 **Recepción** | Wizard en 4 pasos (cliente con buscador, vehículo, problema, confirmar) + lista con filtros |
| 👥 **Clientes** | Buscador, filtros, ficha con visitas/vehículos/comprobantes, activar sin borrar |
| 💬 **Cotización** | Wizard Diagnóstico → Cotización → Aprobar, cálculo en vivo, mecánicos ordenados por carga |
| 🔧 **Órdenes de Trabajo** | Kanban por estado con buscador, reasignar mecánico y cobro directo |
| 📦 **Inventario** | Stock con costo/margen, movimientos auditados (kardex), compra multilínea, mermas, proveedores y alertas |
| 💳 **Pago/Entrega** | Órdenes finalizadas, wizard BOLETA/FACTURA con totales calculados en servidor, ticket/PDF (emisor, detalle, IGV, total en letras, QR), historial con filtros y anulación (solo ADMIN) |
| 👥 **Equipo** | Personal con avatar, especialidad y carga de OT; crear, editar, desactivar y restablecer clave (solo ADMIN) |
| 📊 **Reportes** | 7 pestañas por rol (resumen, clientes, ingresos, inventario, gastos, resultado, mecánicos) con gráficos, exportación CSV y fórmula visible |
| 🎓 **Tutorial y docs** | Caso guiado de 8 pasos, glosario, tours por pantalla y carpeta `docs/` |

![](readme-assets/divider.svg)

## 🧾 Facturación (Boleta/Factura)

El comprobante vive en la base de datos (`comprobante` + `serie_comprobante` con bloqueo pesimista). El wizard de `pago_entrega.html` consume `POST /api/comprobantes` y el total sale de la cotización en el servidor.

- RUC con dígito verificador
- IGV 18 %
- Total en letras
- Folio correlativo por serie (`B001-00000001`, `F001-00000001`)
- Ticket con QR
- Anulación con motivo (solo ADMIN)

> [!WARNING]
> El comprobante es **interno / académico**: no firma XML ni se integra con SUNAT (sin PSE/OSE, sin envío a SUNAT). La facturación electrónica real sería una fase futura con proveedor autorizado.

### Boleta vs Factura en Perú

| | 🧾 **Boleta** | 🧾 **Factura** |
|:--|:--|:--|
| **Para quién** | Consumidor final (persona natural) | Empresa o cliente que necesita crédito fiscal |
| **Documento** | DNI (8) / CE / PAS | RUC (11) obligatorio |
| **Datos extra** | Solo nombre | Razón social + dirección fiscal obligatorias |
| **IGV** | Incluido en el total (referencial) | Desglosado: Subtotal + IGV 18 % + Total |
| **Serie** | `B001` | `F001` |

### Flujo en `pago_entrega.html`

```
OT FINALIZADA
  └─> [Registrar pago] abre el wizard:
       1. Tipo de comprobante: (•) Boleta  ( ) Factura
       2. Documento: DNI 8 (boleta) o RUC 11 + razón social + dirección (factura)
       3. Método de pago: EFECTIVO · YAPE · PLIN · TRANSFERENCIA · TARJETA
       4. Desglose automático: Subtotal + IGV 18 % + Total (= cotización.total)
       5. [Confirmar pago] → POST al backend
  └─> Constancia automática: ticket con serie/número + [Imprimir]
  └─> Tarjeta con badge BOLETA B001-000123 o FACTURA F001-000045
  └─> [Registrar entrega] bloqueado hasta tener pago registrado
  └─> [Ver comprobante] reimprime la constancia en cualquier momento
```

```mermaid
stateDiagram-v2
    [*] --> FINALIZADA
    FINALIZADA --> PAGADA : Registrar pago + comprobante
    PAGADA --> ENTREGADA : Registrar entrega
    ENTREGADA --> [*]
    PAGADA --> ANULADA : Anular comprobante
```

![](readme-assets/divider.svg)

## 🌐 Portal del Cliente

Portal público (`frontend/portal/`) para que el cliente consulte su vehículo sin pasar por el taller. Se identifica con documento y placa (modo A) o con código OTP (modo B). API en `/api/public/portal`.

| Página | Función |
|:--|:--|
| `estado.html` | Estado de la orden de trabajo |
| `cotizacion.html` | Ver cotización |
| `comprobantes.html` | Boletas y facturas emitidas |
| `historial.html` | Historial de visitas |
| `mantenimiento.html` | Próximos mantenimientos |
| `cita.html` | Reservar cita |
| `encuesta.html` | Encuesta de satisfacción |
| `politicas.html` | Políticas y consentimientos |

Tablas del portal (migración `V5__portal_cliente.sql`): `portal_acceso_log`, `otp_codigo`, `cita`, `encuesta_satisfaccion`, `consentimiento`, `solicitud_arco`, `notificacion`.

![](readme-assets/divider.svg)

## 🏗️ Arquitectura del Sistema

```mermaid
flowchart LR
    F["🖼️ Frontend<br/>HTML / CSS / JS<br/>Nginx · JWT"] -->|API| C["🎛️ Controller<br/>REST<br/>@RestController · JWT Filter"]
    C --> S["⚙️ Service<br/>Lógica de negocio<br/>@Service · @Transactional"]
    S --> D[("🗄️ PostgreSQL<br/>Flyway<br/>Docker")]
```

![](readme-assets/divider.svg)

## 🔒 Seguridad

- **Autenticación:** JWT (JSON Web Token)
- **Roles:** ADMIN, MECANICO, ALMACENERO, RECEPCIONISTA
- **Endpoints protegidos** por rol con `@RolesAllowed` (backend) y `ROLE_PAGES` / `ROLE_NAV` (frontend)
- **Contraseñas** hasheadas con BCrypt, distintas por rol en demo
- **Secretos fuera del repo:** `.env` (ver `.env.example`); JWT, claves y modo demo por variables de entorno. Consola H2 solo en desarrollo
- **Estados tipados:** `EstadoOT`, `EstadoRecepcion`, `EstadoCotizacion` (mismo texto en BD, sin estados imposibles)

### Permisos por Rol

| Rol | Puede hacer |
|:--|:--|
| 🛡️ **ADMIN** | Dashboard, recepción, cotización, órdenes, inventario, pago/entrega, comprobantes (emitir y anular), equipo, reportes |
| 🔧 **MECANICO** | Panel mecánico, sus órdenes de trabajo (cambio de estado), inventario en lectura |
| 🧾 **RECEPCIONISTA** | Panel recepcionista, recepción, cotización, órdenes de trabajo, pago/entrega (emitir comprobantes; anular es solo ADMIN) |
| 📦 **ALMACENERO** | Dashboard, inventario (productos, movimientos, alertas de stock) |

Detalle completo en [`docs/permisos.md`](docs/permisos.md).

![](readme-assets/divider.svg)

## 🎨 Tema y Animaciones

### Cambiar tema

Clic en el botón flotante (esquina inferior derecha) para ciclar entre:

- **Sistema** (sigue el tema del SO)
- **Claro**
- **Oscuro**

La preferencia se guarda en `localStorage`.

### Animaciones

- Entrada escalonada (stagger) en indicadores, tablas y cards
- Transiciones de página tipo SPA-light
- Microinteracciones: ripple en botones, pop en badges, elevación en hover
- Skeletons de carga y estados vacíos estilizados
- Textura sutil tipo acero en las superficies
- Flujo visual con puntos animados (pulso)

Todas se desactivan solas si el SO tiene activado `prefers-reduced-motion`.

### Helpers JS (consola del navegador)

```js
agSkeleton(container, 'table')                              // skeleton de carga
agEmptyState(container, null, 'Sin datos', 'Descripción')   // estado vacío
agFlashRow(tableRow)                                        // resalta una fila
```

![](readme-assets/divider.svg)

## 🔌 API REST Endpoints

<details>
<summary><b>Ver endpoints principales</b></summary>

| Método | Endpoint | Descripción |
|:--:|:--|:--|
| `POST` | `/api/auth/login` | Autenticación (devuelve JWT) |
| `POST` | `/api/clientes` | Crear cliente (valida DNI/RUC/CE/pasaporte) |
| `GET` | `/api/clientes` | Listar clientes |
| `GET` | `/api/clientes/buscar?q=` | Buscar paginado por documento, nombre, teléfono o email |
| `PUT` | `/api/clientes/{id}` | Editar cliente (chequea duplicado) |
| `GET` | `/api/clientes/{id}/vehiculos` | Vehículos de un cliente |
| `GET` | `/api/clientes/{id}/resumen` | Ficha con visitas, historial y comprobantes |
| `PATCH` | `/api/clientes/{id}/activo` | Activar / desactivar sin borrar |
| `POST` | `/api/vehiculos` | Crear vehículo (placa peruana, un dueño por placa) |
| `GET` | `/api/vehiculos` | Listar vehículos |
| `POST` | `/api/recepciones` | Crear recepción |
| `GET` | `/api/recepciones` | Listar recepciones (filtro por estado) |
| `POST` | `/api/diagnosticos` | Crear diagnóstico |
| `POST` | `/api/cotizaciones` | Crear cotización |
| `GET` | `/api/cotizaciones` | Listar cotizaciones |
| `PUT` | `/api/cotizaciones/{id}/aprobar` | Aprobar cotización |
| `PUT` | `/api/cotizaciones/{id}/rechazar` | Rechazar cotización |
| `POST` | `/api/ordenes-trabajo` | Crear orden de trabajo |
| `GET` | `/api/ordenes-trabajo` | Listar órdenes de trabajo |
| `PUT` | `/api/ordenes-trabajo/{id}/estado` | Cambiar estado de la OT |
| `PUT` | `/api/ordenes-trabajo/{id}/mecanico` | Reasignar mecánico (OT abierta) |
| `POST` | `/api/ordenes-trabajo/{id}/productos-usados` | Registrar producto usado |
| `GET` | `/api/ordenes-trabajo/{id}/comprobante` | Comprobante vigente de una OT |
| `GET` | `/api/productos` | Listar productos |
| `POST` | `/api/productos` | Crear producto |
| `POST` | `/api/inventario/movimientos` | Registrar movimiento (entrada con costo, consumo, ajuste, merma) |
| `GET` | `/api/inventario/movimientos?tipo=&desde=&hasta=` | Kardex paginado con filtros |
| `GET` | `/api/inventario/alertas` | Alertas de stock bajo |
| `POST` | `/api/proveedores` | Crear proveedor (RUC validado) |
| `POST` | `/api/gastos` | Registrar gasto operativo (solo ADMIN) |
| `POST` | `/api/pagos` | Registrar pago (flujo antiguo, se mantiene) |
| `POST` | `/api/comprobantes` | Emitir boleta/factura (monto desde la cotización) |
| `GET` | `/api/comprobantes` | Listar con filtros y paginación |
| `GET` | `/api/comprobantes/previsualizar` | Totales y adquirente sin reservar número |
| `POST` | `/api/comprobantes/{id}/anular` | Anular con motivo (solo ADMIN) |
| `GET` | `/api/documentos/validar` | Validar DNI/RUC/CE/pasaporte en vivo |
| `POST` | `/api/entregas/{id}` | Registrar entrega |
| `GET` | `/api/reportes/indicadores` | Indicadores del dashboard |
| `GET` | `/api/reportes/clientes-por-dia` | Clientes atendidos por día (+ `/export?formato=csv`) |
| `GET` | `/api/reportes/ingresos?agrupar=dia\|mes` | Ingresos cobrados (solo comprobantes EMITIDO) |
| `GET` | `/api/reportes/resultado` | Utilidad = ventas sin IGV − consumos − gastos |
| `GET` | `/api/reportes/inventario/kardex/{id}` | Kardex con saldos que cuadran |
| `GET` | `/api/reportes/rendimiento-mecanicos` | OT, tiempos y consumo por mecánico |
| `*` | `/api/public/portal/**` | Portal del cliente (público: identificación y OTP) |

</details>

![](readme-assets/divider.svg)

## 🧬 Modelo de Base de Datos

El esquema lo versiona Flyway (`backend/src/main/resources/db/migration/`). Tablas núcleo del flujo del taller:

```
usuario
cliente ──┐
          ├── vehiculo
          │
recepcion ── diagnostico ── cotizacion ─┬─ cotizacion_servicio
                                        └─ cotizacion_producto
                               │
servicio                  orden_trabajo ── ot_producto_usado
producto ─────────────────────┘                │
                                      inventario_movimiento
                                               │
                           pago_entrega ── comprobante / serie_comprobante
```

Además existen tablas de proveedores, gastos operativos y las del [portal del cliente](#-portal-del-cliente). En total el backend mapea 30 entidades JPA.

> [!TIP]
> El pago genera un comprobante interno (serie `B001`/`F001` + correlativo + snapshot fiscal del adquirente) con constancia imprimible. Detalle en [Facturación](#-facturación-boletafactura) y en `CHANGELOG.md`.

![](readme-assets/divider.svg)

## 🛠️ Resolver Problemas Comunes

<details>
<summary><b>"Puerto 8080 ya en uso"</b></summary>

Cierra la otra aplicación que usa ese puerto o cambia el puerto en `docker-compose.yml`:

```yaml
backend:
  ports:
    - "8081:8080"
```

</details>

<details>
<summary><b>"Docker no encontrado"</b></summary>

Instala Docker Desktop desde https://docs.docker.com/get-docker/

</details>

<details>
<summary><b>"Los contenedores no inician"</b></summary>

Verifica que Docker esté corriendo y revisa los logs:

```bash
docker compose logs
```

</details>

<details>
<summary><b>"Los datos se perdieron"</b></summary>

Si eliminaste el volumen `pgdata` se pierden los datos. Con `APP_SEED_DEMO=true` los datos demo se insertan de nuevo al reiniciar.

</details>

<details>
<summary><b>"Schema-validation: missing column" en bucle</b></summary>

Desde la v1.5.1 Flyway aplica solo las migraciones pendientes al arrancar (`baseline` en BD existentes). Si ves este error en una versión vieja, actualiza el código y ejecuta `docker compose up --build` (sin `-v`, así no pierdes datos). Usa `docker compose down -v` solo si aceptas borrar todo.

</details>

<details>
<summary><b>"FlywayException: Unsupported Database: PostgreSQL"</b></summary>

Flyway 10 trae el soporte por base de datos en módulos aparte. Sin `flyway-database-postgresql` en el `pom.xml` el backend no arranca en Docker. Ya viene incluido desde la 2.0.0; si ves el error, tu código es anterior.

</details>

<details>
<summary><b>"Recibo 403 o error CORS"</b></summary>

Asegúrate de que:

1. El frontend se sirve por HTTP (no `file://`).
2. El backend corre en el puerto 8080.
3. Usas exactamente `http://localhost:5500` (no doble clic al HTML ni `127.0.0.1`).

</details>

![](readme-assets/divider.svg)

## ⚠️ Aviso Legal y Parámetros Pendientes

Antes de usar el sistema en producción, el responsable del taller debe definir:

- **Datos de empresa:** `RAZON_SOCIAL`, `RUC_TALLER`, `DIRECCION`, `TELEFONO_WHATSAPP`, `CORREO_CONTACTO`.
- **Protección de datos:** `CORREO_ARCO`, `OFICIAL_DATOS`.
- **Marca / splash:** `SPLASH_TITULO`, `SPLASH_SUBTITULO`, `SPLASH_IMAGEN`.

Mientras no estén definidos, las pantallas que los usan muestran un aviso `TODO`.

- Las políticas (`docs/politicas-README.md`) son borradores técnicos y **requieren revisión legal**.
- La facturación es académica; no incluye integración con SUNAT.

![](readme-assets/divider.svg)

<div align="center">

**Desarrollado para el taller mecánico automotriz** · Proyecto académico UTP

</div>
