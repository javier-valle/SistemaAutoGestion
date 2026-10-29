package com.autogestion.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vista del portal: solo lo que el cliente puede ver de sí mismo.
 * El id del cliente NO viaja: sale del token.
 */
public record PortalResumenDTO(
        String nombre,
        String tipoDocumento,
        String documento,
        Boolean documentoVerificado,
        String fuenteVerificacion,
        String email,
        String telefono,
        java.time.LocalDateTime clienteDesde,
        List<VehiculoDTO> vehiculos,
        List<OrdenDTO> ordenes) {

    public record VehiculoDTO(Long id, String placa, String marca, String modelo, Integer anio) { }

    public record OrdenDTO(Long id, String estado, String placa,
                           LocalDateTime fechaCreacion, LocalDateTime fechaFin) { }
}
