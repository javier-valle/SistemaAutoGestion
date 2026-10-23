package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "diagnostico")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Diagnostico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recepcion_id", nullable = false)
    private Recepcion recepcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mecanico_id", nullable = false)
    private Usuario mecanico;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();
}
