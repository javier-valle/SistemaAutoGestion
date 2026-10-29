package com.autogestion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Calificación de una OT entregada: 1 a 5 estrellas y un comentario opcional. */
@Data
public class PortalEncuestaRequest {

    @NotNull(message = "Indica la orden de trabajo que vas a calificar.")
    private Long ordenTrabajoId;

    @NotNull(message = "Elige una puntuación de 1 a 5.")
    @Min(value = 1, message = "La puntuación va de 1 a 5.")
    @Max(value = 5, message = "La puntuación va de 1 a 5.")
    private Integer puntuacion;

    @Size(max = 1000, message = "El comentario no puede pasar de 1000 caracteres.")
    private String comentario;
}
