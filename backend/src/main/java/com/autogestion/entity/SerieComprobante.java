package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Correlativo por serie. El servicio la bloquea con bloqueo pesimista
 * dentro de la transacción de emisión para que dos cajeros jamás repitan número.
 */
@Entity
@Table(name = "serie_comprobante")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SerieComprobante {

    @Id
    @Column(length = 4)
    private String serie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoComprobante tipo;

    @Column(name = "ultimo_numero", nullable = false)
    @Builder.Default
    private Integer ultimoNumero = 0;
}
