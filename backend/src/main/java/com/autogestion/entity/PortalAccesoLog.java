package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "portal_acceso_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PortalAccesoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "documento_hash", nullable = false, length = 64)
    private String documentoHash;

    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;

    @Column(name = "tipo", nullable = false, length = 10)
    @Builder.Default
    private String tipo = "A";

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private java.time.LocalDateTime creadoEn = java.time.LocalDateTime.now();
}
