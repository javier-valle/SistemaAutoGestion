package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Una línea del comprobante. valor_unitario es SIN IGV (base imponible),
 * precio_unitario es CON IGV (lo que ve el cliente en la cotización).
 */
@Entity
@Table(name = "comprobante_detalle")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComprobanteDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id", nullable = false)
    private Comprobante comprobante;

    @Column(nullable = false)
    private Integer item;

    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 250)
    private String descripcion;

    @Column(name = "unidad_medida", nullable = false, length = 4)
    private String unidadMedida;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "valor_unitario", nullable = false, precision = 12, scale = 4)
    private BigDecimal valorUnitario;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 4)
    private BigDecimal precioUnitario;

    @Column(name = "tipo_afectacion", nullable = false, length = 2)
    @Builder.Default
    private String tipoAfectacion = "10";

    @Column(name = "valor_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorVenta;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(name = "importe_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal importeTotal;
}
