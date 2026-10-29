package com.autogestion.dto;

import java.time.LocalDateTime;

/** Encuesta del portal tal como la revisa el personal del taller. */
public record EncuestaAdminDTO(
        Long id,
        String cliente,
        String documento,
        Long ordenTrabajoId,
        String placa,
        String vehiculo,
        Integer puntuacion,
        String comentario,
        LocalDateTime creadoEn) {
}
