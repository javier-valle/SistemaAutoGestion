package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "producto")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    @Column(name = "stock_actual", nullable = false)
    @Builder.Default
    private Integer stockActual = 0;

    @Column(name = "stock_minimo", nullable = false)
    @Builder.Default
    private Integer stockMinimo = 0;

    /** Último costo de compra: con esto existe margen (precio − costo) y valor del inventario. */
    @Builder.Default
    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "unidad_medida", nullable = false, length = 4)
    private String unidadMedida = "NIU"; // NIU bienes, ZZ servicios

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;
}
