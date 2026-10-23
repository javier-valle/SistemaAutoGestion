package com.autogestion.service;

import com.autogestion.dto.IndicadoresResponse;
import com.autogestion.dto.MovimientoResponseDTO;
import com.autogestion.dto.ReporteDTOs;
import com.autogestion.entity.EstadoComprobante;
import com.autogestion.entity.EstadoOT;
import com.autogestion.entity.TipoMovimiento;
import com.autogestion.entity.CategoriaGasto;
import com.autogestion.entity.OrdenTrabajo;
import com.autogestion.entity.TipoMovimiento;
import com.autogestion.repository.ComprobanteRepository;
import com.autogestion.repository.CotizacionServicioRepository;
import com.autogestion.repository.GastoRepository;
import com.autogestion.repository.InventarioMovimientoRepository;
import com.autogestion.repository.OrdenTrabajoRepository;
import com.autogestion.repository.PagoEntregaRepository;
import com.autogestion.repository.ProductoRepository;
import com.autogestion.repository.RecepcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autogestion.util.AppTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Todo en base de datos (agregaciones JPQL), BigDecimal para dinero y
 * zona America/Lima para los rangos. Excepción documentada: el tiempo
 * promedio por mecánico se promedia en Java porque cada BD resta fechas
 * con sintaxis distinta (EPOCH vs DATEDIFF); los datos ya vienen acotados.
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    public static final ZoneId ZONA = AppTime.ZONA;

    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final ProductoRepository productoRepository;
    private final RecepcionRepository recepcionRepository;
    private final PagoEntregaRepository pagoEntregaRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final CotizacionServicioRepository cotizacionServicioRepository;
    private final InventarioMovimientoRepository movimientoRepository;
    private final GastoRepository gastoRepository;
    private final InventarioService inventarioService;

    /** Rango del mes actual en Lima. */
    public LocalDateTime[] mesActual() {
        YearMonth ym = YearMonth.now(ZONA);
        return new LocalDateTime[]{ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay()};
    }

    /* ---------- Indicadores (dashboard) ---------- */

    @Transactional(readOnly = true)
    public IndicadoresResponse obtenerIndicadores() {
        LocalDateTime[] mes = mesActual();
        // Ingresos = lo realmente cobrado (comprobantes EMITIDO del mes), no la cotización.
        BigDecimal ingresos = comprobanteRepository.ingresosEntre(EstadoComprobante.EMITIDO, mes[0], mes[1]);
        List<OrdenTrabajo> finalizadas = ordenTrabajoRepository.finalizadasEntre(EstadoOT.FINALIZADA, mes[0], mes[1]);
        double promedio = finalizadas.stream()
                .filter(ot -> ot.getFechaFin() != null)
                .mapToLong(ot -> Duration.between(ot.getFechaCreacion(), ot.getFechaFin()).toHours())
                .average().orElse(0.0);
        return IndicadoresResponse.builder()
                .otCompletadasMes((long) finalizadas.size())
                .tiempoPromedioHoras(Math.round(promedio * 10.0) / 10.0)
                .ingresosMes(ingresos.setScale(2, RoundingMode.HALF_UP).doubleValue())
                .productosBajoStock(productoRepository.countConStockBajo())
                .build();
    }

    /* ---------- Clientes ---------- */

    /** H2 y Postgres devuelven el CAST a fecha con tipos distintos; se aceptan todos. */
    static LocalDate comoDia(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof LocalDateTime dt) return dt.toLocalDate();
        if (o instanceof java.util.Date d) {
            return d.toInstant().atZone(ZONA).toLocalDate();
        }
        return LocalDate.parse(o.toString());
    }

    @Transactional(readOnly = true)
    public List<ReporteDTOs.DiaClientes> clientesPorDia(LocalDate desde, LocalDate hastaExcluido) {
        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hastaExcluido.atStartOfDay();
        Map<LocalDate, long[]> mapa = new LinkedHashMap<>();
        for (Object[] r : recepcionRepository.clientesPorDia(d, h)) {
            LocalDate dia = comoDia(r[0]);
            mapa.computeIfAbsent(dia, k -> new long[3]);
            mapa.get(dia)[0] = ((Number) r[1]).longValue();
            mapa.get(dia)[1] = ((Number) r[2]).longValue();
        }
        for (Object[] r : pagoEntregaRepository.entregasPorDia(d, h)) {
            LocalDate dia = comoDia(r[0]);
            mapa.computeIfAbsent(dia, k -> new long[3]);
            mapa.get(dia)[2] = ((Number) r[1]).longValue();
        }
        return mapa.entrySet().stream()
                .map(e -> new ReporteDTOs.DiaClientes(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReporteDTOs.EstadoCount> recepcionesPorEstado() {
        return recepcionRepository.porEstado().stream()
                .map(r -> new ReporteDTOs.EstadoCount(r[0].toString(), ((Number) r[1]).longValue()))
                .toList();
    }

    /* ---------- Ingresos y comprobantes ---------- */

    @Transactional(readOnly = true)
    public List<ReporteDTOs.IngresoPunto> ingresos(LocalDate desde, LocalDate hastaExcluido, String agrupar) {
        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hastaExcluido.atStartOfDay();
        if ("mes".equalsIgnoreCase(agrupar)) {
            return comprobanteRepository.ingresosPorMes(EstadoComprobante.EMITIDO, d, h).stream()
                    .map(r -> new ReporteDTOs.IngresoPunto(
                            String.format("%04d-%02d", ((Number) r[0]).intValue(), ((Number) r[1]).intValue()),
                            (BigDecimal) r[2]))
                    .toList();
        }
        return comprobanteRepository.ingresosPorDia(EstadoComprobante.EMITIDO, d, h).stream()
                .map(r -> new ReporteDTOs.IngresoPunto(r[0].toString(), (BigDecimal) r[1]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReporteDTOs.ComprobanteResumen> comprobantesResumen(LocalDate desde, LocalDate hastaExcluido) {
        return comprobanteRepository.resumenPorTipo(EstadoComprobante.EMITIDO, desde.atStartOfDay(), hastaExcluido.atStartOfDay()).stream()
                .map(r -> new ReporteDTOs.ComprobanteResumen(
                        r[0].toString(), ((Number) r[1]).longValue(),
                        (BigDecimal) r[2], (BigDecimal) r[3], (BigDecimal) r[4]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReporteDTOs.ServicioTop> serviciosMasPedidos(LocalDate desde, LocalDate hastaExcluido, int top) {
        return cotizacionServicioRepository
                .topServicios(desde.atStartOfDay(), hastaExcluido.atStartOfDay(), PageRequest.of(0, top)).stream()
                .map(r -> new ReporteDTOs.ServicioTop((String) r[0], ((Number) r[1]).longValue(), (BigDecimal) r[2]))
                .toList();
    }

    /* ---------- Mecánicos ---------- */

    @Transactional(readOnly = true)
    public List<ReporteDTOs.MecanicoRendimiento> rendimientoMecanicos(LocalDate desde, LocalDate hastaExcluido) {
        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hastaExcluido.atStartOfDay();
        Map<Long, Agg> agg = new LinkedHashMap<>();
        Map<Long, String> nombres = new LinkedHashMap<>();
        for (Object[] r : ordenTrabajoRepository.asignadasPorMecanico(d, h)) {
            Long id = ((Number) r[0]).longValue();
            nombres.put(id, (String) r[1]);
            agg.computeIfAbsent(id, k -> new Agg()).asignadas = ((Number) r[2]).longValue();
        }
        for (Object[] r : ordenTrabajoRepository.enProcesoPorMecanico(java.util.List.of(EstadoOT.FINALIZADA, EstadoOT.CANCELADA))) {
            Long id = ((Number) r[0]).longValue();
            nombres.put(id, (String) r[1]);
            agg.computeIfAbsent(id, k -> new Agg()).enProceso = ((Number) r[2]).longValue();
        }
        Map<Long, List<Long>> minutos = new LinkedHashMap<>();
        for (OrdenTrabajo ot : ordenTrabajoRepository.finalizadasEntre(EstadoOT.FINALIZADA, d, h)) {
            Long id = ot.getMecanico().getId();
            nombres.putIfAbsent(id, ot.getMecanico().getNombreCompleto());
            agg.computeIfAbsent(id, k -> new Agg()).finalizadas++;
            if (ot.getFechaFin() != null && ot.getFechaCreacion() != null) {
                minutos.computeIfAbsent(id, k -> new ArrayList<>())
                        .add(Duration.between(ot.getFechaCreacion(), ot.getFechaFin()).toMinutes());
            }
        }
        Map<Long, BigDecimal[]> consumo = new LinkedHashMap<>();
        for (Object[] r : movimientoRepository.consumoPorMecanico(TipoMovimiento.CONSUMO, d, h)) {
            consumo.put(((Number) r[0]).longValue(), new BigDecimal[]{(BigDecimal) r[3]});
        }
        Map<Long, BigDecimal> ingr = new LinkedHashMap<>();
        for (Object[] r : comprobanteRepository.ingresosPorMecanico(EstadoComprobante.EMITIDO, d, h)) {
            ingr.put(((Number) r[0]).longValue(), (BigDecimal) r[2]);
        }
        List<ReporteDTOs.MecanicoRendimiento> out = new ArrayList<>();
        for (var e : agg.entrySet()) {
            Long id = e.getKey();
            Agg a = e.getValue();
            String nombre = nombres.getOrDefault(id, "Mecánico " + id);
            List<Long> mins = minutos.getOrDefault(id, List.of());
            Double prom = mins.isEmpty() ? null : Math.round((mins.stream().mapToLong(Long::longValue).average().orElse(0) / 60.0) * 10.0) / 10.0;
            out.add(new ReporteDTOs.MecanicoRendimiento(id, nombre, a.asignadas, a.enProceso, a.finalizadas, prom,
                    consumo.getOrDefault(id, new BigDecimal[]{BigDecimal.ZERO})[0],
                    ingr.getOrDefault(id, BigDecimal.ZERO)));
        }
        return out;
    }

    private static class Agg {
        long asignadas, enProceso, finalizadas;
    }

    /* ---------- Inventario ---------- */

    @Transactional(readOnly = true)
    public ReporteDTOs.InventarioResumen inventarioResumen(LocalDate desde, LocalDate hastaExcluido) {
        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hastaExcluido.atStartOfDay();
        return new ReporteDTOs.InventarioResumen(
                movimientoRepository.sumaCostoPorTipo(TipoMovimiento.ENTRADA, d, h),
                movimientoRepository.sumaCostoPorTipo(TipoMovimiento.CONSUMO, d, h),
                movimientoRepository.sumaCostoPorTipo(TipoMovimiento.MERMA, d, h)
                        .add(movimientoRepository.sumaCostoPorTipo(TipoMovimiento.AJUSTE_NEGATIVO, d, h)),
                productoRepository.valorInventario(),
                productoRepository.countConStockBajo());
    }

    @Transactional(readOnly = true)
    public List<ReporteDTOs.ConsumoGrupo> consumo(LocalDate desde, LocalDate hastaExcluido, String agrupar, int top) {
        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hastaExcluido.atStartOfDay();
        List<Object[]> filas = switch (agrupar == null ? "producto" : agrupar) {
            case "ot" -> movimientoRepository.consumoPorOT(TipoMovimiento.CONSUMO, d, h, PageRequest.of(0, top));
            case "mecanico" -> movimientoRepository.consumoPorMecanico(TipoMovimiento.CONSUMO, d, h).stream()
                    .map(r -> new Object[]{r[0], r[1], r[2]}).toList();
            default -> movimientoRepository.consumoPorProducto(TipoMovimiento.CONSUMO, d, h, PageRequest.of(0, top));
        };
        return filas.stream()
                .map(r -> new ReporteDTOs.ConsumoGrupo((String) r[0], ((Number) r[1]).longValue(), (BigDecimal) r[2]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReporteDTOs.ProductoUso> productosMasUsados(LocalDate desde, LocalDate hastaExcluido, int top) {
        return movimientoRepository.consumoPorProducto(TipoMovimiento.CONSUMO, desde.atStartOfDay(), hastaExcluido.atStartOfDay(),
                PageRequest.of(0, top)).stream()
                .map(r -> new ReporteDTOs.ProductoUso((String) r[0], ((Number) r[1]).longValue(), (BigDecimal) r[2]))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReporteDTOs.MargenDTO> margenPorProducto() {
        return productoRepository.preciosVsCostos().stream()
                .map(r -> {
                    BigDecimal precio = (BigDecimal) r[1];
                    BigDecimal costo = (BigDecimal) r[2];
                    BigDecimal margen = (precio != null && costo != null) ? precio.subtract(costo) : BigDecimal.ZERO;
                    return new ReporteDTOs.MargenDTO((String) r[0], precio, costo, margen);
                }).toList();
    }

    @Transactional(readOnly = true)
    public ReporteDTOs.KardexDTO kardex(Long productoId, LocalDate desde, LocalDate hastaExcluido) {
        var movs = movimientoRepository.kardex(productoId, desde.atStartOfDay(), hastaExcluido.atStartOfDay());
        String nombre = movs.isEmpty() ? null : movs.get(0).getProducto().getNombre();
        int saldo = 0;
        List<ReporteDTOs.KardexLinea> lineas = new ArrayList<>();
        for (var m : movs) {
            boolean suma = m.getTipo() == TipoMovimiento.ENTRADA || m.getTipo() == TipoMovimiento.AJUSTE_POSITIVO;
            saldo += suma ? m.getCantidad() : -m.getCantidad();
            lineas.add(new ReporteDTOs.KardexLinea(m.getFecha(), m.getTipo().name(), m.getCantidad(),
                    m.getCostoUnitario(), m.getDocumentoRef(),
                    m.getProveedor() != null ? m.getProveedor().getNombre() : null,
                    m.getUsuario() != null ? m.getUsuario().getNombreCompleto() : null,
                    m.getMotivo(), saldo));
        }
        // Saldo inicial = actual − neto del rango (cuadra con stock_actual por construcción).
        int neto = saldo;
        int actual = movs.isEmpty() ? 0 : movs.get(0).getProducto().getStockActual();
        return new ReporteDTOs.KardexDTO(productoId, nombre, actual - neto, lineas, actual);
    }

    /* ---------- Entradas (compras) ---------- */

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<MovimientoResponseDTO> entradas(
            LocalDate desde, LocalDate hastaExcluido, Long proveedorId, int page, int size) {
        return inventarioService.entradas(desde, hastaExcluido, proveedorId, page, size);
    }

    /* ---------- Gastos y resultado ---------- */

    @Transactional(readOnly = true)
    public List<ReporteDTOs.GastoGrupo> gastosPorCategoria(LocalDate desde, LocalDate hasta) {
        return gastoRepository.porCategoria(desde, hasta).stream()
                .map(r -> new ReporteDTOs.GastoGrupo(r[0].toString(),
                        ((com.autogestion.entity.CategoriaGasto) r[0]).getEtiqueta(),
                        ((Number) r[1]).longValue(), (BigDecimal) r[2]))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReporteDTOs.ResultadoDTO resultado(LocalDate desde, LocalDate hastaExcluido) {
        LocalDateTime d = desde.atStartOfDay();
        LocalDateTime h = hastaExcluido.atStartOfDay();
        BigDecimal ingresos = comprobanteRepository.ingresosEntre(EstadoComprobante.EMITIDO, d, h);
        BigDecimal sinIgv = comprobanteRepository.ingresosSinIgvEntre(EstadoComprobante.EMITIDO, d, h);
        BigDecimal compras = movimientoRepository.sumaCostoPorTipo(TipoMovimiento.ENTRADA, d, h);
        BigDecimal consumido = movimientoRepository.sumaCostoPorTipo(TipoMovimiento.CONSUMO, d, h);
        BigDecimal gastos = gastoRepository.totalEntre(desde, hastaExcluido.minusDays(1));
        BigDecimal mermas = movimientoRepository.sumaCostoPorTipo(TipoMovimiento.MERMA, d, h)
                .add(movimientoRepository.sumaCostoPorTipo(TipoMovimiento.AJUSTE_NEGATIVO, d, h));
        BigDecimal utilidadAntesMermas = sinIgv.subtract(consumido).subtract(gastos);
        BigDecimal utilidadEstimada = utilidadAntesMermas.subtract(mermas);
        return new ReporteDTOs.ResultadoDTO(ingresos, sinIgv, compras, consumido, gastos,
                mermas, utilidadAntesMermas, utilidadEstimada);
    }

    @Transactional(readOnly = true)
    public List<ReporteDTOs.EstadoCount> stockBajo() {
        long n = productoRepository.countConStockBajo();
        return List.of(new ReporteDTOs.EstadoCount("BAJO_STOCK", n));
    }
}
