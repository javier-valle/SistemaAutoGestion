package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cita")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(name = "fecha", nullable = false)
    private java.time.LocalDateTime fecha;

    @Column(name = "franja", nullable = false, length = 20)
    private String franja;

    @Column(name = "servicio", length = 255)
    private String servicio;

    @Column(name = "comentario", length = 500)
    private String comentario;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "SOLICITADA";

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private java.time.LocalDateTime creadoEn = java.time.LocalDateTime.now();
}
