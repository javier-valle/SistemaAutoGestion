package com.autogestion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Registros livianos para reportes (se serializan directo a JSON/CSV). */
public final class ReporteDTOs {

    private ReporteDTOs() { }

    /** "Cliente atendido" = con recepción registrada ese día; entregado va aparte. */
    public record DiaClientes(LocalDate dia, long clientesAtendidos, long recepciones, long entregas) { }

    public record IngresoPunto(String periodo, BigDecimal total) { }

    public record ComprobanteResumen(String tipo, long cantidad, BigDecimal gravado, BigDecimal igv, BigDecimal total) { }

    public record EstadoCount(String estado, long cantidad) { }

    public record ServicioTop(String nombre, long veces, BigDecimal total) { }

    public record MecanicoRendimiento(Long id, String nombre, long asignadas, long enProceso,
                                      long finalizadas, Double horasPromedio,
                                      BigDecimal consumoCosto, BigDecimal ingresos) { }

    public record InventarioResumen(BigDecimal totalComprado, BigDecimal totalConsumidoAlCosto,
                                    BigDecimal totalMermas, BigDecimal valorInventarioActual,
                                    long productosBajoStock) { }

    public record ConsumoGrupo(String clave, long cantidad, BigDecimal costo) { }

    public record KardexLinea(LocalDateTime fecha, String tipo, int cantidad, BigDecimal costoUnitario,
                              String documento, String proveedor, String usuario, String motivo,
                              int saldo) { }

    public record KardexDTO(Long productoId, String productoNombre, int saldoInicial,
                            List<KardexLinea> movimientos, int saldoFinal) { }

    public record GastoGrupo(String categoria, String etiqueta, long cantidad, BigDecimal total) { }

    /**
     * utilidadEstimada = ingresosSinIgv − costoConsumido − gastosOperativos.
     * Estimación académica, no es un estado financiero contable.
     */
    public record ResultadoDTO(BigDecimal ingresos, BigDecimal ingresosSinIgv, BigDecimal compras,
                               BigDecimal costoConsumido, BigDecimal gastosOperativos,
                               BigDecimal totalMermas, BigDecimal utilidadAntesMermas,
                               BigDecimal utilidadEstimada) { }

    public record ProductoUso(String nombre, long cantidad, BigDecimal costo) { }

    public record MargenDTO(String nombre, BigDecimal precio, BigDecimal costo, BigDecimal margen) { }
}
