## Cómo agregar un reporte nuevo

1. Crear `Seccion` en `ReporteService.java` (o nuevo método) que devuelva datos del rango.
2. Agregar caso al `switch` de `ReporteExportService.agregar*()` (PDF + Excel consumen el mismo modelo).
3. Agregar tab en `TABS` del frontend (`reportes.html`) con `roles` correspondientes.
4. Crear función `tabX()` que declare datos + columnas y llame `renderTabla()` y/o `svgBars()`.
5. Asegurar que `esc()` se use en todo dato dinámico de `innerHTML`.
6. Actualizar esta guía, `CHANGELOG.md` y `README.md` (tabla de endpoints).
