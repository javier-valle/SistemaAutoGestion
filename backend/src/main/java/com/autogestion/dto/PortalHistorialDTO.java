package com.autogestion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Una visita del historial: la OT cerrada, el vehículo y lo que se hizo.
 * El total es el de la cotización aprobada; el comprobante va aparte porque
 * puede existir la OT sin boleta emitida.
 */
public record PortalHistorialDTO(
        Long ordenTrabajoId,
        String placa,
        String vehiculo,
        LocalDateTime fechaIngreso,
        LocalDateTime fechaFin,
        String estado,
        BigDecimal total,
        String comprobanteFolio,
        List<String> servicios) {
}
