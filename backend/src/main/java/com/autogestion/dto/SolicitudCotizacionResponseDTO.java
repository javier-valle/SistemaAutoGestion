package com.autogestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Vista de una solicitud de cotización, sin datos que no correspondan al cliente. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudCotizacionResponseDTO {
    private Long id;
    private Long vehiculoId;
    private String vehiculoPlaca;
    private String clienteNombre;
    private String descripcion;
    private String estado;
    private String notaTaller;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
}
