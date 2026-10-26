package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notificacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "destinatario_hash", nullable = false, length = 64)
    private String destinatarioHash;

    @Column(name = "canal", nullable = false, length = 20)
    @Builder.Default
    private String canal = "WHATSAPP";

    @Column(name = "plantilla", nullable = false, length = 100)
    private String plantilla;

    @Column(name = "contenido", length = 2000)
    private String contenido;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private java.time.LocalDateTime creadoEn = java.time.LocalDateTime.now();
}
