package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cliente")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String documento;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 10)
    private TipoDocumento tipoDocumento = TipoDocumento.DNI;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    /** Género opcional (MASCULINO/FEMENINO/OTROS): personaliza la vista. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Genero genero;

    @Column(length = 250)
    private String direccion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    /**
     * true = una fuente externa devolvió el nombre. No es una verificación
     * oficial de identidad. Por defecto false: un cliente creado por el
     * personal o por registro manual no está verificado.
     */
    @Builder.Default
    @Column(name = "documento_verificado", nullable = false)
    private Boolean documentoVerificado = false;

    /** Nombre legible de la fuente que devolvió el nombre (NULL si fue declarado). */
    @Column(name = "fuente_verificacion", length = 120)
    private String fuenteVerificacion;

    @Builder.Default
    @Column(name = "creado_en", nullable = false)
    private java.time.LocalDateTime creadoEn = java.time.LocalDateTime.now();

    /** Nombre a imprimir: razón social en empresas, nombre en personas. */
    public String nombreFiscal() {
        if (tipoDocumento == TipoDocumento.RUC && razonSocial != null && !razonSocial.isBlank()) {
            return razonSocial;
        }
        return nombre;
    }
}
