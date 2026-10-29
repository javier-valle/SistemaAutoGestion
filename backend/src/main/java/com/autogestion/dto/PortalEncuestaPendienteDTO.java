package com.autogestion.dto;

import java.time.LocalDateTime;

/** OT entregada que todavía no tiene encuesta: es la que el portal ofrece calificar. */
public record PortalEncuestaPendienteDTO(
        Long ordenTrabajoId,
        String placa,
        String vehiculo,
        LocalDateTime fechaFin) {
}
