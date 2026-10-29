package com.autogestion.service;

import com.autogestion.dto.ReporteDTOs;
import com.autogestion.dto.ReporteTablaRequest;
import com.autogestion.util.Rangos;
import com.autogestion.util.ReportePdfGenerator;
import com.autogestion.util.ReporteExcelGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import static com.autogestion.util.ReporteExcelGenerator.Columna.dinero;
import static com.autogestion.util.ReporteExcelGenerator.Columna.entero;
import static com.autogestion.util.ReporteExcelGenerator.Columna.texto;
import com.autogestion.dto.RangoFechas;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReporteExportService {

    private final ReporteService reporteService;

    /**
     * Rango de los archivos exportados: las fechas llegan como las escribe el
     * usuario (ambas inclusivas) y se normalizan aquí, una sola vez.
     */
    public byte[] exportarConsolidado(List<String> reportesSeleccionados,
                                       String formato,
                                       LocalDate desde, LocalDate hasta,
                                       String rol) {
        List<ReportePdfGenerator.Seccion> seccionesPdf = new ArrayList<>();
        List<ReporteExcelGenerator.Seccion> seccionesXlsx = new ArrayList<>();
        LocalDate[] rango = Rangos.rangoReporte(desde, hasta);
        LocalDate ini = rango[0];
        LocalDate fin = rango[1];

        for (String r : reportesSeleccionados) {
            switch (r) {
                case "resumen" -> agregarResumen(seccionesPdf, seccionesXlsx, ini, fin);
                case "clientes" -> agregarClientes(seccionesPdf, seccionesXlsx, ini, fin);
                case "ingresos" -> agregarIngresos(seccionesPdf, seccionesXlsx, ini, fin);
                case "inventario" -> agregarInventario(seccionesPdf, seccionesXlsx, ini, fin);
                case "gastos" -> agregarGastos(seccionesPdf, seccionesXlsx, ini, fin);
                case "resultado" -> agregarResultado(seccionesPdf, seccionesXlsx, ini, fin);
                case "mecanicos" -> agregarMecanicos(seccionesPdf, seccionesXlsx, ini, fin);
            }
        }

        String titulo = "Reporte consolidado de AutoGestión";
        if (formato.equalsIgnoreCase("pdf")) {
            return ReportePdfGenerator.generar(seccionesPdf, titulo, ini, fin.minusDays(1), rol);
        } else {
            return ReporteExcelGenerator.generar(seccionesXlsx, titulo, ini, fin.minusDays(1), rol);
        }
    }

    /**
     * Exporta una tabla de la pantalla con el mismo estilo de los reportes:
     * Excel con encabezado fijo, formato por columna y área de impresión, o PDF
     * A4 horizontal. Los valores llegan como texto o número y aquí se normalizan.
     */
    public byte[] exportarTabla(ReporteTablaRequest request, String formato,
                                LocalDate desde, LocalDate hasta, String rol) {
        LocalDate[] rango = Rangos.rangoReporte(desde, hasta);
        LocalDate ini = rango[0];
        LocalDate fin = rango[1].minusDays(1);
        String titulo = recortar(request.getTitulo(), 120);
        String nota = recortar(request.getNota(), 300);

        List<ReporteExcelGenerator.Columna> columnas = new ArrayList<>();
        for (ReporteTablaRequest.Columna c : request.getColumnas()) {
            columnas.add(new ReporteExcelGenerator.Columna(
                    recortar(c.getTitulo(), 80), tipoDe(c.getTipo()), c.isTotaliza()));
        }

        List<Object[]> filasExcel = new ArrayList<>();
        List<String[]> filasPdf = new ArrayList<>();
        for (List<Object> fila : request.getFilas()) {
            Object[] paraExcel = new Object[columnas.size()];
            String[] paraPdf = new String[columnas.size()];
            for (int i = 0; i < columnas.size(); i++) {
                Object valor = fila != null && i < fila.size() ? fila.get(i) : null;
                Object normalizado = normalizar(valor, columnas.get(i).tipo());
                paraExcel[i] = normalizado;
                paraPdf[i] = textoPdf(normalizado, columnas.get(i).tipo());
            }
            filasExcel.add(paraExcel);
            filasPdf.add(paraPdf);
        }

        if ("pdf".equalsIgnoreCase(formato)) {
            String[] encabezados = columnas.stream()
                    .map(ReporteExcelGenerator.Columna::titulo)
                    .toArray(String[]::new);
            return ReportePdfGenerator.generar(
                    List.of(new ReportePdfGenerator.Seccion(titulo, filasPdf, encabezados, nota)),
                    titulo, ini, fin, rol);
        }

        return ReporteExcelGenerator.generarTabla(
                new ReporteExcelGenerator.Seccion(titulo, columnas, filasExcel, nota),
                titulo, ini, fin, rol);
    }

    private static ReporteExcelGenerator.Tipo tipoDe(String tipo) {
        if (tipo == null) return ReporteExcelGenerator.Tipo.TEXTO;
        return switch (tipo.trim().toUpperCase(Locale.ROOT)) {
            case "ENTERO" -> ReporteExcelGenerator.Tipo.ENTERO;
            case "DINERO" -> ReporteExcelGenerator.Tipo.DINERO;
            case "DECIMAL" -> ReporteExcelGenerator.Tipo.DECIMAL;
            case "PROMEDIO" -> ReporteExcelGenerator.Tipo.PROMEDIO;
            default -> ReporteExcelGenerator.Tipo.TEXTO;
        };
    }

    /** Acepta números del JSON y también texto tipo "S/ 1,234.50" o "12,5". */
    private static Object normalizar(Object valor, ReporteExcelGenerator.Tipo tipo) {
        if (valor == null) return null;
        String texto = valor.toString().trim();
        if (texto.isEmpty() || "-".equals(texto) || "—".equals(texto)) return null;
        if (tipo == ReporteExcelGenerator.Tipo.TEXTO) return recortar(texto, 250);

        if (valor instanceof Number n) {
            return tipo == ReporteExcelGenerator.Tipo.ENTERO ? n.longValue() : new BigDecimal(n.toString());
        }

        String limpio = texto.replace("S/", "").replace(" ", "").replace("%", "");
        if (limpio.contains(",") && limpio.contains(".")) {
            limpio = limpio.replace(",", "");
        } else if (limpio.contains(",") && limpio.matches("[^,]*,[0-9]{1,2}")) {
            limpio = limpio.replace(",", ".");
        }
        try {
            return tipo == ReporteExcelGenerator.Tipo.ENTERO
                    ? (Object) Long.parseLong(limpio)
                    : (Object) new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            return recortar(texto, 250);
        }
    }

    private static String textoPdf(Object valor, ReporteExcelGenerator.Tipo tipo) {
        if (valor == null) return "—";
        if (valor instanceof BigDecimal bd) {
            return tipo == ReporteExcelGenerator.Tipo.DINERO ? money(bd) : bd.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        if (valor instanceof Number n) {
            return tipo == ReporteExcelGenerator.Tipo.ENTERO
                    ? String.format(Locale.ROOT, "%,d", n.longValue())
                    : money(new BigDecimal(n.toString()));
        }
        return valor.toString();
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private void agregarResumen(List<ReportePdfGenerator.Seccion> pdf,
                                 List<ReporteExcelGenerator.Seccion> xlsx,
                                 LocalDate desde, LocalDate hasta) {
        var ind = reporteService.obtenerIndicadores();
        var cli = reporteService.clientesPorDia(desde, hasta);
        var comp = reporteService.comprobantesResumen(desde, hasta);
        var res = reporteService.resultado(desde, hasta);

        String[] enc = {"Indicador", "Valor"};
        List<String[]> datos = new ArrayList<>();
        datos.add(new String[]{"OT completadas (mes)", String.valueOf(ind.getOtCompletadasMes())});
        datos.add(new String[]{"Ingresos del mes (cobrado)", money(ind.getIngresosMes())});
        datos.add(new String[]{"Productos bajo stock", String.valueOf(ind.getProductosBajoStock())});
        datos.add(new String[]{"Clientes atendidos (rango)", String.valueOf(cli.stream().mapToLong(ReporteDTOs.DiaClientes::clientesAtendidos).sum())});
        pdf.add(new ReportePdfGenerator.Seccion("Resumen - Indicadores", datos, enc, "Valores del mes actual y rango seleccionado."));

        List<Object[]> datosX = new ArrayList<>();
        datosX.add(new Object[]{"OT completadas (mes)", String.valueOf(ind.getOtCompletadasMes())});
        datosX.add(new Object[]{"Ingresos del mes (cobrado)", money(ind.getIngresosMes())});
        datosX.add(new Object[]{"Productos bajo stock", String.valueOf(ind.getProductosBajoStock())});
        datosX.add(new Object[]{"Clientes atendidos (rango)", String.valueOf(cli.stream().mapToLong(ReporteDTOs.DiaClientes::clientesAtendidos).sum())});
        xlsx.add(new ReporteExcelGenerator.Seccion("Resumen - Indicadores",
                List.of(texto("Indicador"), texto("Valor")), datosX,
                "Valores del mes actual y rango seleccionado."));
    }

    private void agregarClientes(List<ReportePdfGenerator.Seccion> pdf,
                                  List<ReporteExcelGenerator.Seccion> xlsx,
                                  LocalDate desde, LocalDate hasta) {
        var dias = reporteService.clientesPorDia(desde, hasta);
        var estados = reporteService.recepcionesPorEstado();

        String[] enc = {"Día", "Atendidos", "Recepciones", "Entregas"};
        List<String[]> datos = new ArrayList<>();
        for (var d : dias) {
            datos.add(new String[]{d.dia().toString(), String.valueOf(d.clientesAtendidos()), String.valueOf(d.recepciones()), String.valueOf(d.entregas())});
        }
        pdf.add(new ReportePdfGenerator.Seccion("Clientes por día", datos, enc, "Clientes con recepción registrada en el rango."));

        List<Object[]> datosX = new ArrayList<>();
        for (var d : dias) {
            datosX.add(new Object[]{d.dia(), d.clientesAtendidos(), d.recepciones(), d.entregas()});
        }
        xlsx.add(new ReporteExcelGenerator.Seccion("Clientes por día",
                List.of(texto("Día"), entero("Atendidos", true), entero("Recepciones", true),
                        entero("Entregas", true)),
                datosX, "Clientes con recepción registrada en el rango."));
    }

    private void agregarIngresos(List<ReportePdfGenerator.Seccion> pdf,
                                  List<ReporteExcelGenerator.Seccion> xlsx,
                                  LocalDate desde, LocalDate hasta) {
        var pts = reporteService.ingresos(desde, hasta, "dia");
        String[] enc = {"Periodo", "Total (S/)"};
        List<String[]> datos = new ArrayList<>();
        List<Object[]> datosX = new ArrayList<>();
        for (var p : pts) {
            datos.add(new String[]{p.periodo(), money(p.total())});
            datosX.add(new Object[]{p.periodo(), p.total()});
        }
        pdf.add(new ReportePdfGenerator.Seccion("Ingresos por período", datos, enc, "Solo comprobantes EMITIDO (cobrado)."));
        xlsx.add(new ReporteExcelGenerator.Seccion("Ingresos por período",
                List.of(texto("Periodo"), dinero("Total (S/)", true)), datosX,
                "Solo comprobantes EMITIDO (cobrado)."));
    }

    private void agregarInventario(List<ReportePdfGenerator.Seccion> pdf,
                                    List<ReporteExcelGenerator.Seccion> xlsx,
                                    LocalDate desde, LocalDate hasta) {
        var resumen = reporteService.inventarioResumen(desde, hasta);
        String[] enc = {"Concepto", "Valor"};
        List<String[]> datos = new ArrayList<>();
        datos.add(new String[]{"Total comprado", money(resumen.totalComprado())});
        datos.add(new String[]{"Total consumido al costo", money(resumen.totalConsumidoAlCosto())});
        datos.add(new String[]{"Total mermas", money(resumen.totalMermas())});
        datos.add(new String[]{"Valor inventario actual", money(resumen.valorInventarioActual())});
        datos.add(new String[]{"Productos bajo stock", String.valueOf(resumen.productosBajoStock())});
        pdf.add(new ReportePdfGenerator.Seccion("Inventario - Resumen", datos, enc, "Valores monetarios en soles (PEN)."));

        List<Object[]> datosX = new ArrayList<>();
        datosX.add(new Object[]{"Total comprado", resumen.totalComprado()});
        datosX.add(new Object[]{"Total consumido al costo", resumen.totalConsumidoAlCosto()});
        datosX.add(new Object[]{"Total mermas", resumen.totalMermas()});
        datosX.add(new Object[]{"Valor inventario actual", resumen.valorInventarioActual()});
        datosX.add(new Object[]{"Productos bajo stock", resumen.productosBajoStock()});
        xlsx.add(new ReporteExcelGenerator.Seccion("Inventario - Resumen",
                List.of(texto("Concepto"), texto("Valor")), datosX,
                "Valores monetarios en soles (PEN)."));
    }

    private void agregarGastos(List<ReportePdfGenerator.Seccion> pdf,
                                List<ReporteExcelGenerator.Seccion> xlsx,
                                LocalDate desde, LocalDate hasta) {
        var grupos = reporteService.gastosPorCategoria(desde, hasta.minusDays(1));
        String[] enc = {"Categoría", "Etiqueta", "Veces", "Total (S/)"};
        List<String[]> datos = new ArrayList<>();
        List<Object[]> datosX = new ArrayList<>();
        for (var g : grupos) {
            datos.add(new String[]{g.categoria(), g.etiqueta(), String.valueOf(g.cantidad()), money(g.total())});
            datosX.add(new Object[]{g.categoria(), g.etiqueta(), g.cantidad(), g.total()});
        }
        pdf.add(new ReportePdfGenerator.Seccion("Gastos por categoría", datos, enc, "Gastos operativos registrados en el rango."));
        xlsx.add(new ReporteExcelGenerator.Seccion("Gastos por categoría",
                List.of(texto("Categoría"), texto("Etiqueta"), entero("Veces", true),
                        dinero("Total (S/)", true)), datosX,
                "Gastos operativos registrados en el rango."));
    }

    private void agregarResultado(List<ReportePdfGenerator.Seccion> pdf,
                                   List<ReporteExcelGenerator.Seccion> xlsx,
                                   LocalDate desde, LocalDate hasta) {
        var r = reporteService.resultado(desde, hasta);
        String[] enc = {"Concepto", "Monto (S/)"};
        List<String[]> datos = new ArrayList<>();
        datos.add(new String[]{"Ingresos (con IGV)", money(r.ingresos())});
        datos.add(new String[]{"Ingresos sin IGV", money(r.ingresosSinIgv())});
        datos.add(new String[]{"Compras a proveedores", money(r.compras())});
        datos.add(new String[]{"Costo consumido", money(r.costoConsumido())});
        datos.add(new String[]{"Gastos operativos", money(r.gastosOperativos())});
        datos.add(new String[]{"Utilidad estimada", money(r.utilidadEstimada())});
        pdf.add(new ReportePdfGenerator.Seccion("Resultado estimado", datos, enc, "Estimación académica, no es estado financiero contable."));

        List<Object[]> datosX = new ArrayList<>();
        datosX.add(new Object[]{"Ingresos (con IGV)", r.ingresos()});
        datosX.add(new Object[]{"Ingresos sin IGV", r.ingresosSinIgv()});
        datosX.add(new Object[]{"Compras a proveedores", r.compras()});
        datosX.add(new Object[]{"Costo consumido", r.costoConsumido()});
        datosX.add(new Object[]{"Gastos operativos", r.gastosOperativos()});
        datosX.add(new Object[]{"Utilidad estimada", r.utilidadEstimada()});
        xlsx.add(new ReporteExcelGenerator.Seccion("Resultado estimado",
                List.of(texto("Concepto"), dinero("Monto (S/)", false)), datosX,
                "Estimación académica, no es estado financiero contable."));
    }

    private void agregarMecanicos(List<ReportePdfGenerator.Seccion> pdf,
                                   List<ReporteExcelGenerator.Seccion> xlsx,
                                   LocalDate desde, LocalDate hasta) {
        var filas = reporteService.rendimientoMecanicos(desde, hasta);
        String[] enc = {"Mecánico", "Asignadas", "En proceso", "Finalizadas", "Horas prom", "Consumo (S/)", "Ingresos (S/)"};
        List<String[]> datos = new ArrayList<>();
        List<Object[]> datosX = new ArrayList<>();
        for (var m : filas) {
            datos.add(new String[]{
                    m.nombre(),
                    String.valueOf(m.asignadas()),
                    String.valueOf(m.enProceso()),
                    String.valueOf(m.finalizadas()),
                    m.horasPromedio() != null ? String.format(Locale.ROOT, "%.1f", m.horasPromedio()) : "—",
                    money(m.consumoCosto()),
                    money(m.ingresos())
            });
            datosX.add(new Object[]{m.nombre(), m.asignadas(), m.enProceso(), m.finalizadas(),
                    m.horasPromedio(), m.consumoCosto(), m.ingresos()});
        }
        pdf.add(new ReportePdfGenerator.Seccion("Rendimiento de mecánicos", datos, enc, "Datos del rango seleccionado."));
        xlsx.add(new ReporteExcelGenerator.Seccion("Rendimiento de mecánicos",
                List.of(texto("Mecánico"), entero("Asignadas", true), entero("En proceso", true),
                        entero("Finalizadas", true), new ReporteExcelGenerator.Columna("Horas prom.",
                                ReporteExcelGenerator.Tipo.DECIMAL, false),
                        dinero("Consumo (S/)", true), dinero("Ingresos (S/)", true)), datosX,
                "Datos del rango seleccionado."));
    }


    private static String money(Object valor) {
        if (valor == null) return "S/ 0.00";
        BigDecimal v = valor instanceof BigDecimal b ? b : new BigDecimal(valor.toString());
        return "S/ " + new java.text.DecimalFormat("#,##0.00").format(v);
    }

}
