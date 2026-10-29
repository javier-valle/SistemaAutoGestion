package com.autogestion.service;

import com.autogestion.dto.ReporteDTOs;
import com.autogestion.util.Rangos;
import com.autogestion.util.ReportePdfGenerator;
import com.autogestion.util.ReporteExcelGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import com.autogestion.dto.RangoFechas;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReporteExportService {

    private final ReporteService reporteService;

    public byte[] exportarConsolidado(List<String> reportesSeleccionados,
                                       String formato,
                                       LocalDate desde, LocalDate hasta,
                                       String rol) {
        List<ReportePdfGenerator.Seccion> seccionesPdf = new ArrayList<>();
        List<ReporteExcelGenerator.Seccion> seccionesXlsx = new ArrayList<>();

        for (String r : reportesSeleccionados) {
            switch (r) {
                case "resumen" -> agregarResumen(seccionesPdf, seccionesXlsx, desde, hasta);
                case "clientes" -> agregarClientes(seccionesPdf, seccionesXlsx, desde, hasta);
                case "ingresos" -> agregarIngresos(seccionesPdf, seccionesXlsx, desde, hasta);
                case "inventario" -> agregarInventario(seccionesPdf, seccionesXlsx, desde, hasta);
                case "gastos" -> agregarGastos(seccionesPdf, seccionesXlsx, desde, hasta);
                case "resultado" -> agregarResultado(seccionesPdf, seccionesXlsx, desde, hasta);
                case "mecanicos" -> agregarMecanicos(seccionesPdf, seccionesXlsx, desde, hasta);
            }
        }

        String titulo = "Reporte consolidado de AutoGestión";
        if (formato.equalsIgnoreCase("pdf")) {
            return ReportePdfGenerator.generar(seccionesPdf, titulo, desde, hasta, rol);
        } else {
            return ReporteExcelGenerator.generar(seccionesXlsx, titulo, desde, hasta, rol);
        }
    }

    private void agregarResumen(List<ReportePdfGenerator.Seccion> pdf,
                                 List<ReporteExcelGenerator.Seccion> xlsx,
                                 LocalDate desde, LocalDate hasta) {
        var rango = Rangos.rangoReporte(desde, hasta);
        var ind = reporteService.obtenerIndicadores();
        var cli = reporteService.clientesPorDia(rango[0], rango[1]);
        var comp = reporteService.comprobantesResumen(rango[0], rango[1]);
        var res = reporteService.resultado(rango[0], rango[1]);

        String[] enc = {"Indicador", "Valor"};
        List<String[]> datos = new ArrayList<>();
        datos.add(new String[]{"OT completadas (mes)", String.valueOf(ind.getOtCompletadasMes())});
        datos.add(new String[]{"Ingresos del mes (cobrado)", money(ind.getIngresosMes())});
        datos.add(new String[]{"Productos bajo stock", String.valueOf(ind.getProductosBajoStock())});
        datos.add(new String[]{"Clientes atendidos (rango)", String.valueOf(cli.stream().mapToLong(ReporteDTOs.DiaClientes::clientesAtendidos).sum())});
        pdf.add(new ReportePdfGenerator.Seccion("Resumen - Indicadores", datos, enc, "Valores del mes actual y rango seleccionado."));

        List<Object[]> datosX = new ArrayList<>();
        datosX.add(new Object[]{"OT completadas (mes)", ind.getOtCompletadasMes()});
        datosX.add(new Object[]{"Ingresos del mes (cobrado)", ind.getIngresosMes()});
        datosX.add(new Object[]{"Productos bajo stock", ind.getProductosBajoStock()});
        datosX.add(new Object[]{"Clientes atendidos (rango)", cli.stream().mapToLong(ReporteDTOs.DiaClientes::clientesAtendidos).sum()});
        xlsx.add(new ReporteExcelGenerator.Seccion("Resumen - Indicadores", enc, datosX, "Valores del mes actual y rango seleccionado."));
    }

    private void agregarClientes(List<ReportePdfGenerator.Seccion> pdf,
                                  List<ReporteExcelGenerator.Seccion> xlsx,
                                  LocalDate desde, LocalDate hasta) {
        var rango = Rangos.rangoReporte(desde, hasta);
        var dias = reporteService.clientesPorDia(rango[0], rango[1]);
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
        xlsx.add(new ReporteExcelGenerator.Seccion("Clientes por día", enc, datosX, "Clientes con recepción registrada en el rango."));
    }

    private void agregarIngresos(List<ReportePdfGenerator.Seccion> pdf,
                                  List<ReporteExcelGenerator.Seccion> xlsx,
                                  LocalDate desde, LocalDate hasta) {
        var rango = Rangos.rangoReporte(desde, hasta);
        var pts = reporteService.ingresos(rango[0], rango[1], "dia");
        String[] enc = {"Periodo", "Total (S/)"};
        List<String[]> datos = new ArrayList<>();
        List<Object[]> datosX = new ArrayList<>();
        for (var p : pts) {
            datos.add(new String[]{p.periodo(), money(p.total())});
            datosX.add(new Object[]{p.periodo(), p.total()});
        }
        pdf.add(new ReportePdfGenerator.Seccion("Ingresos por período", datos, enc, "Solo comprobantes EMITIDO (cobrado)."));
        xlsx.add(new ReporteExcelGenerator.Seccion("Ingresos por período", enc, datosX, "Solo comprobantes EMITIDO (cobrado)."));
    }

    private void agregarInventario(List<ReportePdfGenerator.Seccion> pdf,
                                    List<ReporteExcelGenerator.Seccion> xlsx,
                                    LocalDate desde, LocalDate hasta) {
        var rango = Rangos.rangoReporte(desde, hasta);
        var resumen = reporteService.inventarioResumen(rango[0], rango[1]);
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
        xlsx.add(new ReporteExcelGenerator.Seccion("Inventario - Resumen", enc, datosX, "Valores monetarios en soles (PEN)."));
    }

    private void agregarGastos(List<ReportePdfGenerator.Seccion> pdf,
                                List<ReporteExcelGenerator.Seccion> xlsx,
                                LocalDate desde, LocalDate hasta) {
        var rango = Rangos.rangoReporte(desde, hasta);

        var grupos = reporteService.gastosPorCategoria(rango[0], rango[1].minusDays(1));
        String[] enc = {"Categoría", "Etiqueta", "Veces", "Total (S/)"};
        List<String[]> datos = new ArrayList<>();
        List<Object[]> datosX = new ArrayList<>();
        for (var g : grupos) {
            datos.add(new String[]{g.categoria(), g.etiqueta(), String.valueOf(g.cantidad()), money(g.total())});
            datosX.add(new Object[]{g.categoria(), g.etiqueta(), g.cantidad(), g.total()});
        }
        pdf.add(new ReportePdfGenerator.Seccion("Gastos por categoría", datos, enc, "Gastos operativos registrados en el rango."));
        xlsx.add(new ReporteExcelGenerator.Seccion("Gastos por categoría", enc, datosX, "Gastos operativos registrados en el rango."));
    }

    private void agregarResultado(List<ReportePdfGenerator.Seccion> pdf,
                                   List<ReporteExcelGenerator.Seccion> xlsx,
                                   LocalDate desde, LocalDate hasta) {
        var rango = Rangos.rangoReporte(desde, hasta);
        var r = reporteService.resultado(rango[0], rango[1]);
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
        xlsx.add(new ReporteExcelGenerator.Seccion("Resultado estimado", enc, datosX, "Estimación académica, no es estado financiero contable."));
    }

    private void agregarMecanicos(List<ReportePdfGenerator.Seccion> pdf,
                                   List<ReporteExcelGenerator.Seccion> xlsx,
                                   LocalDate desde, LocalDate hasta) {
        var rango = Rangos.rangoReporte(desde, hasta);
        var filas = reporteService.rendimientoMecanicos(rango[0], rango[1]);
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
        xlsx.add(new ReporteExcelGenerator.Seccion("Rendimiento de mecánicos", enc, datosX, "Datos del rango seleccionado."));
    }


    private static String money(Object valor) {
        if (valor == null) return "S/ 0.00";
        BigDecimal v = valor instanceof BigDecimal b ? b : new BigDecimal(valor.toString());
        return "S/ " + new java.text.DecimalFormat("#,##0.00").format(v);
    }

}
