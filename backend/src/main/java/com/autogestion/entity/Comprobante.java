package com.autogestion.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cabecera de boleta/factura. Los datos de emisor y adquirente se copian
 * al emitir (snapshot): aunque el cliente cambie después, el comprobante
 * impreso siempre muestra lo que era verdad ese día.
 */
@Entity
@Table(name = "comprobante")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_trabajo_id", nullable = false)
    private OrdenTrabajo ordenTrabajo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoComprobante tipo;

    @Column(name = "codigo_sunat", nullable = false, length = 2)
    private String codigoSunat;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(nullable = false)
    private Integer numero;

    @Builder.Default
    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision = LocalDateTime.now();

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String moneda = "PEN";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago", nullable = false, length = 10)
    private FormaPago formaPago = FormaPago.CONTADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private MetodoPago metodoPago;

    // Emisor (snapshot)
    @Column(name = "emisor_ruc", nullable = false, length = 11)
    private String emisorRuc;

    @Column(name = "emisor_razon_social", nullable = false, length = 200)
    private String emisorRazonSocial;

    @Column(name = "emisor_nombre_comercial", length = 200)
    private String emisorNombreComercial;

    @Column(name = "emisor_direccion", nullable = false, length = 250)
    private String emisorDireccion;

    @Column(name = "emisor_ubigeo", length = 6)
    private String emisorUbigeo;

    @Column(name = "emisor_telefono", length = 30)
    private String emisorTelefono;

    @Column(name = "emisor_email", length = 150)
    private String emisorEmail;

    // Adquirente (snapshot)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "cliente_tipo_doc", nullable = false, length = 10)
    private String clienteTipoDoc;

    @Column(name = "cliente_num_doc", nullable = false, length = 12)
    private String clienteNumDoc;

    @Column(name = "cliente_nombre", nullable = false, length = 200)
    private String clienteNombre;

    @Column(name = "cliente_direccion", length = 250)
    private String clienteDireccion;

    // Importes (BigDecimal, 2 decimales; nunca double)
    @Column(name = "total_gravado", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGravado;

    @Builder.Default
    @Column(name = "total_exonerado", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalExonerado = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_inafecto", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalInafecto = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDescuento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "total_letras", nullable = false, length = 250)
    private String totalLetras;

    @Column(length = 500)
    private String observacion;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoComprobante estado = EstadoComprobante.EMITIDO;

    @Column(name = "motivo_anulacion", length = 250)
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emitido_por", nullable = false)
    private Usuario emitidoPor;

    @OneToMany(mappedBy = "comprobante", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ComprobanteDetalle> detalle = new ArrayList<>();

    /** Folio estilo SUNAT: F001-00000001 (8 dígitos). */
    public String folio() {
        return serie + "-" + String.format("%08d", numero);
    }

    /** Contenido del QR según ficha SUNAT (hash vacío hasta tener firma). */
    public String qrContenido() {
        return String.join("|",
                emisorRuc, codigoSunat, serie, String.valueOf(numero),
                igv.toPlainString(), total.toPlainString(),
                fechaEmision.toLocalDate().toString(), clienteTipoDoc, clienteNumDoc, "");
    }
}
