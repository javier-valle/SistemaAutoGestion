package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orden_trabajo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mecanico_id", nullable = false)
    private Usuario mecanico;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOT estado = EstadoOT.PENDIENTE;

    @Column(name = "fecha_creacion", nullable = false)
    @Builder.Default
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;
}
