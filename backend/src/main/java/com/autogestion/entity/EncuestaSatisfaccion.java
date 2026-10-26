package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "encuesta_satisfaccion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EncuestaSatisfaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "orden_trabajo_id")
    private OrdenTrabajo ordenTrabajo;

    @Column(name = "puntuacion", nullable = false)
    private Integer puntuacion;

    @Column(name = "comentario", length = 1000)
    private String comentario;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private java.time.LocalDateTime creadoEn = java.time.LocalDateTime.now();
}
