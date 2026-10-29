package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Petición del cliente para que el taller revise su vehículo y prepare una
 * cotización. No contiene precios: el presupuesto real lo arma el personal
 * después de la recepción y el diagnóstico, para que el cliente no pueda
 * fijar importes a su nombre.
 */
@Entity
@Table(name = "solicitud_cotizacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SolicitudCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitudCotizacion estado = EstadoSolicitudCotizacion.PENDIENTE;

    @Column(name = "nota_taller", length = 500)
    private String notaTaller;

    @Builder.Default
    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Builder.Default
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();
}
