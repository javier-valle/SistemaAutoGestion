package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "otp_codigo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpCodigo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "documento", nullable = false, length = 20)
    private String documento;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    private String tipoDocumento;

    @Column(name = "codigo", nullable = false, length = 10)
    private String codigo;

    @Column(name = "hash_codigo", nullable = false, length = 64)
    private String hashCodigo;

    @Column(name = "expira_en", nullable = false)
    private java.time.LocalDateTime expiraEn;

    @Column(name = "intentos", nullable = false)
    @Builder.Default
    private Integer intentos = 0;

    @Column(name = "usado", nullable = false)
    @Builder.Default
    private Boolean usado = false;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private java.time.LocalDateTime creadoEn = java.time.LocalDateTime.now();
}
