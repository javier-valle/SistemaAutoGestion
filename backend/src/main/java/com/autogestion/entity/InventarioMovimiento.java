package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cada movimiento guarda foto completa: stock antes/después, costo,
 * quién lo hizo y a qué OT o proveedor pertenece. Con esto el kardex
 * cuadra: saldo inicial + entradas − salidas = saldo final.
 */
@Entity
@Table(name = "inventario_movimiento")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventarioMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 255)
    private String motivo;

    @Column(name = "costo_unitario", precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_trabajo_id")
    private OrdenTrabajo ordenTrabajo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "documento_ref", length = 60)
    private String documentoRef;

    @Column(name = "stock_antes")
    private Integer stockAntes;

    @Column(name = "stock_despues")
    private Integer stockDespues;

    @Column(nullable = false)
    @Builder.Default`r`nprivate LocalDateTime fecha = LocalDateTime.now();
}
