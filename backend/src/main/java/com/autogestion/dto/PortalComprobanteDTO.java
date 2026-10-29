package com.autogestion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Comprobante visto desde el portal: lo justo para listar y descargar el PDF. */
public record PortalComprobanteDTO(
        Long id,
        Long ordenTrabajoId,
        String tipo,
        String folio,
        LocalDateTime fechaEmision,
        BigDecimal total,
        String estado,
        String placa) {
}
