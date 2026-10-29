package com.autogestion.controller;

import com.autogestion.dto.IndicadoresResponse;
import com.autogestion.dto.ReporteDTOs;
import com.autogestion.dto.ReporteTablaRequest;
import com.autogestion.service.ReporteService;
import com.autogestion.service.ReporteExportService;
import com.autogestion.util.Rangos;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Roles: ADMIN ve todo; RECEPCIONISTA solo clientes y estados;
 * ALMACENERO solo stock-bajo (más indicadores, que ya tenía).
 * Rango por defecto: mes actual. Fechas como yyyy-MM-dd.
 */
@RolesAllowed("ADMIN")
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final ReporteExportService reporteExportService;


    @RolesAllowed({"ADMIN", "ALMACENERO"})
    @GetMapping("/indicadores")
    public ResponseEntity<IndicadoresResponse> obtenerIndicadores() {
        return ResponseEntity.ok(reporteService.obtenerIndicadores());
    }

    @RolesAllowed({"ADMIN", "RECEPCIONISTA"})
    @GetMapping("/clientes-por-dia")
    public ResponseEntity<List<ReporteDTOs.DiaClientes>> clientesPorDia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        return ResponseEntity.ok(reporteService.clientesPorDia(r[0], r[1]));
    }

    @RolesAllowed({"ADMIN", "RECEPCIONISTA"})
    @GetMapping("/clientes-por-dia/export")
    public ResponseEntity<byte[]> exportarClientesPorDia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "csv") String formato) {
        if (!"csv".equalsIgnoreCase(formato)) {
            return ResponseEntity.badRequest().build();
        }
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        StringBuilder csv = new StringBuilder("Día;Clientes atendidos;Recepciones;Entregas\n");
        for (var f : reporteService.clientesPorDia(r[0], r[1])) {
            csv.append(f.dia()).append(';').append(f.clientesAtendidos()).append(';')
                    .append(f.recepciones()).append(';').append(f.entregas()).append('\n');
        }
        byte[] contenido = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=clientes-por-dia.csv")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(contenido);
    }

    @RolesAllowed({"ADMIN", "RECEPCIONISTA"})
    @GetMapping("/recepciones-por-estado")
    public ResponseEntity<List<ReporteDTOs.EstadoCount>> recepcionesPorEstado() {
        return ResponseEntity.ok(reporteService.recepcionesPorEstado());
    }

    @GetMapping("/ingresos")
    public ResponseEntity<List<ReporteDTOs.IngresoPunto>> ingresos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "dia") String agrupar) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        return ResponseEntity.ok(reporteService.ingresos(r[0], r[1], agrupar));
    }

    @GetMapping("/comprobantes-resumen")
    public ResponseEntity<List<ReporteDTOs.ComprobanteResumen>> comprobantesResumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        return ResponseEntity.ok(reporteService.comprobantesResumen(r[0], r[1]));
    }

    @GetMapping("/servicios-mas-pedidos")
    public ResponseEntity<List<ReporteDTOs.ServicioTop>> serviciosMasPedidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int top) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        return ResponseEntity.ok(reporteService.serviciosMasPedidos(r[0], r[1], Math.min(50, Math.max(1, top))));
    }

    @GetMapping("/rendimiento-mecanicos")
    public ResponseEntity<List<ReporteDTOs.MecanicoRendimiento>> rendimientoMecanicos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        return ResponseEntity.ok(reporteService.rendimientoMecanicos(r[0], r[1]));
    }

    @RolesAllowed({"ADMIN", "ALMACENERO"})
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<ReporteDTOs.EstadoCount>> stockBajo() {
        return ResponseEntity.ok(reporteService.stockBajo());
    }

    @RolesAllowed({"ADMIN", "ALMACENERO"})
    @GetMapping("/inventario/resumen")
    public ResponseEntity<ReporteDTOs.InventarioResumen> inventarioResumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        return ResponseEntity.ok(reporteService.inventarioResumen(r[0], r[1]));
    }

    @RolesAllowed({"ADMIN", "ALMACENERO"})
    @GetMapping("/inventario/consumo")
    public ResponseEntity<List<ReporteDTOs.ConsumoGrupo>> consumo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "producto") String agrupar,
            @RequestParam(defaultValue = "10") int top) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        return ResponseEntity.ok(reporteService.consumo(r[0], r[1], agrupar, Math.min(50, Math.max(1, top))));
    }

    @RolesAllowed({"ADMIN", "ALMACENERO"})
    @GetMapping("/inventario/kardex/{productoId}")
    public ResponseEntity<ReporteDTOs.KardexDTO> kardex(
            @PathVariable Long productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        return ResponseEntity.ok(reporteService.kardex(productoId, r[0], r[1]));
    }

    @RolesAllowed({"ADMIN", "ALMACENERO"})
    @GetMapping("/inventario/entradas")
    public ResponseEntity<?> entradas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        return ResponseEntity.ok(reporteService.entradas(r[0], r[1], proveedorId, page, size));
    }

    @GetMapping("/gastos")
    public ResponseEntity<List<ReporteDTOs.GastoGrupo>> gastos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);

        return ResponseEntity.ok(reporteService.gastosPorCategoria(r[0], r[1].minusDays(1)));
    }

    @GetMapping("/resultado")
    public ResponseEntity<ReporteDTOs.ResultadoDTO> resultado(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        return ResponseEntity.ok(reporteService.resultado(r[0], r[1]));
    }

    @RolesAllowed({"ADMIN", "ALMACENERO"})
    @GetMapping("/productos-mas-usados")
    public ResponseEntity<List<ReporteDTOs.ProductoUso>> productosMasUsados(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int top) {
        LocalDate[] r = Rangos.rangoReporte(desde, hasta);
        return ResponseEntity.ok(reporteService.productosMasUsados(r[0], r[1], Math.min(50, Math.max(1, top))));
    }

    @RolesAllowed({"ADMIN", "ALMACENERO"})
    @GetMapping("/margen-por-producto")
    public ResponseEntity<List<ReporteDTOs.MargenDTO>> margenPorProducto() {
        return ResponseEntity.ok(reporteService.margenPorProducto());
    }



    @RolesAllowed({"ADMIN", "RECEPCIONISTA", "ALMACENERO"})
    @GetMapping("/export/disponibles")
    public ResponseEntity<List<String>> reportesDisponibles() {
        return ResponseEntity.ok(List.of(
                "resumen", "clientes", "ingresos", "inventario",
                "gastos", "resultado", "mecanicos"));
    }

    /**
     * Exporta una tabla tal como se ve en pantalla. El cliente manda títulos,
     * tipos y valores; el formato (dinero, entero, decimal) y el diseño los
     * pone el backend, para que el archivo salga igual que el reporte impreso.
     */
    @RolesAllowed("ADMIN")
    @PostMapping("/export/tabla")
    public ResponseEntity<byte[]> exportarTabla(
            @Valid @RequestBody ReporteTablaRequest request,
            @RequestParam(defaultValue = "excel") String formato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        boolean pdf = "pdf".equalsIgnoreCase(formato);
        boolean excel = "excel".equalsIgnoreCase(formato) || "xlsx".equalsIgnoreCase(formato);
        if (!pdf && !excel) {
            return ResponseEntity.badRequest()
                    .body(("Formato no válido: " + formato).getBytes(StandardCharsets.UTF_8));
        }
        byte[] archivo = reporteExportService.exportarTabla(request, pdf ? "pdf" : "excel",
                desde, hasta, rolActual());
        String nombre = archivo(request.getTitulo(), pdf ? "pdf" : "xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(pdf ? MediaType.APPLICATION_PDF
                        : MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    }

    @RolesAllowed("ADMIN")
    @GetMapping("/export/consolidado")
    public ResponseEntity<byte[]> exportarConsolidado(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "resumen,clientes,ingresos,inventario,gastos,resultado,mecanicos") String reportes,
            @RequestParam(defaultValue = "pdf") String formato) {
        if (!formato.equalsIgnoreCase("pdf") && !formato.equalsIgnoreCase("excel")) {
            return ResponseEntity.badRequest().body(("Formato no válido: " + formato).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        List<String> seleccionados = Arrays.stream(reportes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (seleccionados.isEmpty()) {
            return ResponseEntity.badRequest().body("No se seleccionó ningún reporte.".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        byte[] archivo = reporteExportService.exportarConsolidado(seleccionados, formato, desde, hasta,
                rolActual());
        String ext = formato.equalsIgnoreCase("pdf") ? "pdf" : "xlsx";
        String mime = formato.equalsIgnoreCase("pdf") ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-consolidado." + ext)
                .contentType(MediaType.parseMediaType(mime))
                .body(archivo);
    }

    private static String rolActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "ADMIN" : auth.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .map(a -> a.replace("ROLE_", ""))
                .orElse("ADMIN");
    }

    /** Nombre de archivo seguro: sin acentos, espacios ni caracteres raros. */
    private static String archivo(String titulo, String ext) {
        String base = titulo == null ? "reporte" : titulo.toLowerCase(Locale.ROOT)
                .replaceAll("[áàä]", "a").replaceAll("[éèë]", "e")
                .replaceAll("[íìï]", "i").replaceAll("[óòö]", "o")
                .replaceAll("[úùü]", "u").replaceAll("[ñ]", "n")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isBlank()) base = "reporte";
        if (base.length() > 50) base = base.substring(0, 50);
        return "autogestion-" + base + "." + ext;
    }
}
