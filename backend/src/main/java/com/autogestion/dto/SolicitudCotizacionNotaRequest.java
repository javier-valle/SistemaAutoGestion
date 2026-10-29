package com.autogestion.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SolicitudCotizacionNotaRequest {

    /** Respuesta breve del taller que el cliente verá en su panel. */
    @Size(max = 500, message = "La nota no puede superar 500 caracteres.")
    private String nota;
}
