package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "solicitud_arco")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SolicitudArco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;

    @Column(name = "verificacion", length = 255)
    private String verificacion;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "RECIBIDA";

    @Column(name = "numero_solicitud", nullable = false, unique = true, length = 32)
    @Builder.Default
    private String numeroSolicitud = java.util.UUID.randomUUID().toString();

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private java.time.LocalDateTime creadoEn = java.time.LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private java.time.LocalDateTime actualizadoEn = java.time.LocalDateTime.now();
}
