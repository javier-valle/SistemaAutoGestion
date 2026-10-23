package com.autogestion.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Ficha completa para el panel de clientes: datos, vehículos, visitas y comprobantes. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ClienteDetalleDTO {
    private Long id;
    private String nombre;
    private String tipoDocumento;
    private String documento;
    private String razonSocial;
    private String telefono;
    private String email;
    private String direccion;
    /** MASCULINO, FEMENINO u OTROS (null = sin especificar). */
    private String genero;
    private Boolean activo;
    private Long visitas;
    private LocalDateTime ultimaVisita;
    private List<VehiculoDTO> vehiculos;
    private List<RecepcionDTO> recepciones;
    private List<ComprobanteDTO> comprobantes;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VehiculoDTO {
        private Long id;
        private String placa;
        private String marca;
        private String modelo;
        private Integer anio;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecepcionDTO {
        private Long id;
        private LocalDateTime fechaIngreso;
        private String estado;
        private String placa;
        private String problema;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ComprobanteDTO {
        private Long id;
        private String folio;
        private String tipo;
        private BigDecimal total;
        private String estado;
        private LocalDateTime fechaEmision;
    }
}
