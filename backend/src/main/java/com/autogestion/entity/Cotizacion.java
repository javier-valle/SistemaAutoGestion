package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cotizacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostico_id", nullable = false)
    private Diagnostico diagnostico;

    /**
     * Igual a diagnostico_id solo mientras la cotización está viva (PENDIENTE,
     * EN_DIAGNOSTICO o APROBADA). El índice único sobre esta columna impide dos
     * cotizaciones vigentes para el mismo diagnóstico; al rechazar o convertir
     * se limpia y el diagnóstico queda libre para renegociar.
     */
    @Column(name = "diagnostico_vigente_id")
    private Long diagnosticoVigenteId;

    @Builder.Default
    @Column(nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCotizacion estado = EstadoCotizacion.PENDIENTE;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();
}
