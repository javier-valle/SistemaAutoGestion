package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "consentimiento")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Consentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "politica_version", nullable = false, length = 20)
    private String politicaVersion;

    @Column(name = "finalidad", nullable = false, length = 50)
    private String finalidad;

    @Column(name = "aceptado", nullable = false)
    @Builder.Default
    private Boolean aceptado = true;

    @Column(name = "fecha_aceptacion", nullable = false)
    @Builder.Default
    private java.time.LocalDateTime fechaAceptacion = java.time.LocalDateTime.now();

    @Column(name = "ip_hash", length = 64)
    private String ipHash;
}
