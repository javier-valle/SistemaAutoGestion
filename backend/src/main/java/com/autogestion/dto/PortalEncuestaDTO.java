package com.autogestion.dto;

import java.time.LocalDateTime;

/** Encuesta ya registrada, para mostrarla en el portal sin duplicar el formulario. */
public record PortalEncuestaDTO(
        Long id,
        Long ordenTrabajoId,
        String placa,
        Integer puntuacion,
        String comentario,
        LocalDateTime creadoEn) {
}
