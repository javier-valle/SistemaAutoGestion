package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recepcion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Recepcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(name = "fecha_ingreso", nullable = false)
    @Builder.Default
    private LocalDateTime fechaIngreso = LocalDateTime.now();

    @Column(name = "problema_reportado", nullable = false, columnDefinition = "TEXT")
    private String problemaReportado;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecepcion estado = EstadoRecepcion.PENDIENTE;

    @Column(name = "nivel_combustible", length = 20)
    private String nivelCombustible;

    @Column(name = "danos_previos", length = 500)
    private String danosPrevios;

    @Column(length = 500)
    private String accesorios;

    private Integer kilometraje;
}
