package com.autogestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SolicitudCotizacionRequest {

    @NotNull(message = "Elige el vehículo de la solicitud.")
    private Long vehiculoId;

    @NotBlank(message = "Cuéntanos qué le pasa al vehículo.")
    @Size(min = 10, max = 800, message = "Describe el problema entre 10 y 800 caracteres.")
    private String descripcion;
}
